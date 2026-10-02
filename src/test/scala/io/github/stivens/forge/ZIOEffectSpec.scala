package io.github.stivens.forge

import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.mixins.*
import io.github.stivens.forge.testsetup.ZIOEffect
import io.github.stivens.forge.testsetup.unsafeRunZIO
import io.github.stivens.forge.testsetup.zioTransactor
import org.scalatest.funspec.AnyFunSpec
import zio.Task
import zio.ZIO
import zio.interop.catz.*

object ZIORepositoryTemplate extends Forge with ZIOEffect

class ZIOEffectSpec extends AnyFunSpec {
  describe("given a Generic repository bound to ZIO") {
    case class User(id: Long, name: String, email: Option[String]) derives Read, Write
    case class UpdateUser(name: Option[String] = None)

    object UserRepository
        extends AbstractRepository.Generic[User](tableName = "users_zio_effect")
        with ZIOEffect
        with IdentifiedBy.Generic[User, Long](_.id)
        with Updates.Generic[User, Long, UpdateUser](
          handleUpdate = toFragments[UpdateUser].usingNonEmpty(_.name)(name => fr"name = $name").compile
        )
        with Deletions.Generic[User, Long] {

      def renameOrFail(id: Long, name: String): Task[Option[User]] =
        transact(getByIdOrFailC(id).flatMap(user => updateC(user.id, UpdateUser(name = Some(name)))))
    }

    unsafeRunZIO(sql"""
        DROP TABLE IF EXISTS users_zio_effect;
        CREATE TABLE users_zio_effect (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255));
    """.update.run.transact(zioTransactor))

    val users = List(
      User(id = 1, name = "John", email = Some("john@example.com")),
      User(id = 2, name = "Jane", email = None)
    )

    unsafeRunZIO(UserRepository.createMany(users))

    it("should return Task and compose with other ZIO effects") {
      val program: Task[(Option[User], Option[User], Long)] = for {
        renamed <- UserRepository.renameOrFail(2L, "Janet")
        deleted <- UserRepository.delete(1L)
        count   <- UserRepository.countAll
      } yield (renamed, deleted, count)

      assert(unsafeRunZIO(program) == (Some(users(1).copy(name = "Janet")), Some(users.head), 1))
    }

    it("should surface database errors in the ZIO error channel") {
      val result = unsafeRunZIO(UserRepository.renameOrFail(99L, "Nobody").either)
      assert(result.left.exists(_.isInstanceOf[EntityNotFoundError[?, ?]]))
    }

    it("should not run until the Task is executed") {
      val pending = UserRepository.create(User(id = 3, name = "Jim", email = None))
      assert(unsafeRunZIO(UserRepository.getById(3L)) == None)
      unsafeRunZIO(pending)
      assert(unsafeRunZIO(UserRepository.getById(3L)).map(_.name) == Some("Jim"))
    }
  }

  describe("given a repository built from a ZIO Forge") {
    case class Movie(id: Long, title: String) derives Read, Write

    object MovieRepository
        extends ZIORepositoryTemplate.Simple[Movie](tableName = "movies_zio_effect")
        with ZIORepositoryTemplate.IdentifiedBy[Movie, Long](_.id)

    unsafeRunZIO(sql"""
        DROP TABLE IF EXISTS movies_zio_effect;
        CREATE TABLE movies_zio_effect (id SERIAL PRIMARY KEY, title VARCHAR(255) NOT NULL);
        INSERT INTO movies_zio_effect (title) VALUES ('Alien'), ('Heat');
    """.update.run.transact(zioTransactor))

    it("should run concurrently with ZIO combinators") {
      val titles = ZIO.foreachPar(List(1L, 2L))(MovieRepository.getById).map(_.flatten.map(_.title))
      assert(unsafeRunZIO(titles) == List("Alien", "Heat"))
    }
  }
}
