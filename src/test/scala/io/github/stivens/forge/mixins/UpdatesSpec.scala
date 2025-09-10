package io.github.stivens.forge.mixins

import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.testsetup.transactor
import io.github.stivens.forge.toFragments
import org.scalatest.funspec.AnyFunSpec

class UpdatesSpec extends AnyFunSpec {
  describe("given repository with Updates mixin") {
    case class User(id: Long, name: String, email: String, address: String) derives Read, Write

    case class UpdateUser(name: Option[String] = None, email: Option[String] = None, address: Option[String] = None)

    object UserRepository
        extends AbstractRepository.Simple[User](fr"users_to_be_updated")
        with IdentifiedBy[User, Long](_.id)
        with Updates[User, Long, UpdateUser](
          handleUpdate = toFragments[UpdateUser]
            .usingNonEmpty(_.name)(name => fr"name = ${name}")
            .usingNonEmpty(_.email)(email => fr"email = ${email}")
            .usingNonEmpty(_.address)(address => fr"address = ${address}")
            .compile
        )

    // init relation
    sql"""
        DROP TABLE IF EXISTS users_to_be_updated;
        CREATE TABLE users_to_be_updated (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, address VARCHAR(255) NOT NULL);
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val users = List(
      User(id = 1, name = "John", email = "john@example.com", address = "123 Main St"),
      User(id = 2, name = "Jane", email = "jane@example.com", address = "456 Main St"),
      User(id = 3, name = "Jim", email = "jim@example.com", address = "789 Main St"),
      User(id = 4, name = "Jill", email = "jill@example.com", address = "101 Main St")
    )

    UserRepository.createMany(users).transact(transactor).unsafeRunSync()

    it("should update by id") {
      val update       = UpdateUser(name = Some("John Doe"), email = Some("john.doe@example.com"), address = Some("Modified Address"))
      val updateResult = UserRepository.update(1L, update).transact(transactor).unsafeRunSync()
      val getAllResult = UserRepository.getAll.transact(transactor).unsafeRunSync().sortBy(_.id)

      val updatedUser = User(id = 1, name = "John Doe", email = "john.doe@example.com", address = "Modified Address")

      assert(updateResult == Some(updatedUser))
      assert(getAllResult == users.updated(0, updatedUser))
    }

    it("should update many by ids") {
      val update       = UpdateUser(address = Some("New Address"))
      val updateResult = UserRepository.updateMany(users.map(_.id), update).transact(transactor).unsafeRunSync().sortBy(_.id)
      val getAllResult = UserRepository.getAll.transact(transactor).unsafeRunSync().sortBy(_.id)

      val updatedUsers = users
        .updated(0, User(id = 1, name = "John Doe", email = "john.doe@example.com", address = "Modified Address"))
        .map(_.copy(address = "New Address"))

      assert(updateResult == updatedUsers)
      assert(getAllResult == updatedUsers)
    }
  }
}
