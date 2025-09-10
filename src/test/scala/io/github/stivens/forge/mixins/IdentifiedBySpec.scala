package io.github.stivens.forge.mixins

import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.EntityNotFoundError
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

import scala.util.Failure
import scala.util.Try

class IdentifiedBySpec extends AnyFunSpec {
  describe("given view with IdentifiedBy mixin") {
    case class User(id: Long, name: String, email: String) derives Read, Write

    object UserRepository extends AbstractRepository.Simple[User](fr"users") with IdentifiedBy[User, Long](_.id)

    // init relation
    sql"""
        DROP TABLE IF EXISTS users;
        CREATE TABLE users (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            email VARCHAR(255) NOT NULL
        );
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val users = List(
      User(id = 1, name = "John", email = "john@example.com"),
      User(id = 2, name = "Jane", email = "jane@example.com"),
      User(id = 3, name = "Jim", email = "jim@example.com"),
      User(id = 4, name = "Jill", email = "jill@example.com")
    )

    UserRepository.createMany(users).transact(transactor).unsafeRunSync()

    it("should get by id") {
      val getByIdResult       = UserRepository.getById(1).transact(transactor).unsafeRunSync()
      val getByIdOrFailResult = UserRepository.getByIdOrFail(1).transact(transactor).unsafeRunSync()

      val expectedUser = User(id = 1, name = "John", email = "john@example.com")

      assert(getByIdResult == Some(expectedUser))
      assert(getByIdOrFailResult == expectedUser)
    }

    it("should get many by ids") {
      val getManyByIdsResult      = UserRepository.getManyByIds(List(1L, 2L, 3L)).transact(transactor).unsafeRunSync()
      val getManyByIdsToMapResult = UserRepository.getManyByIdsToMap(List(1L, 2L, 3L)).transact(transactor).unsafeRunSync()

      val expectedUsers = users.filter(_.id <= 3L)

      assert(getManyByIdsResult == expectedUsers)
      assert(getManyByIdsToMapResult == Map(1L -> users(0), 2L -> users(1), 3L -> users(2)))
    }

    it("should fail if entity not found") {
      val result = Try(UserRepository.getByIdOrFail(42).transact(transactor).unsafeRunSync())

      assert(result == Failure(EntityNotFoundError[User, Long](42)))
    }
  }
}
