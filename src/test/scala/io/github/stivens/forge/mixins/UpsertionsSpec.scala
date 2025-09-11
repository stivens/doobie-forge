package io.github.stivens.forge.mixins

import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

class UpsertionsSpec extends AnyFunSpec {
  describe("given repository with Upsertions mixin") {
    case class User(id: Long, name: String, email: String) derives Read, Write

    object UserRepository
        extends AbstractRepository.Simple[User](fr"users_to_be_upserted")
        with IdentifiedBy[User, Long](_.id)
        with Upsertions[User]

    // init relation
    sql"""
        DROP TABLE IF EXISTS users_to_be_upserted;
        CREATE TABLE users_to_be_upserted (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL);
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val users = List(
      User(id = 1, name = "John", email = "john@example.com"),
      User(id = 2, name = "Jane", email = "jane@example.com"),
      User(id = 3, name = "Jim", email = "jim@example.com"),
      User(id = 4, name = "Jill", email = "jill@example.com")
    )

    UserRepository.createMany(users).transact(transactor).unsafeRunSync()

    it("should upsert an entity") {
      val newJohn = User(id = 1, name = "John Doe", email = "john.doe@example.com")
      val upsertResult =
        UserRepository.upsert(newJohn).transact(transactor).unsafeRunSync()
      val getAllResult = UserRepository.getAll.transact(transactor).unsafeRunSync().sortBy(_.id)

      assert(upsertResult == newJohn)
      assert(getAllResult == users.updated(0, newJohn))
    }
  }
}
