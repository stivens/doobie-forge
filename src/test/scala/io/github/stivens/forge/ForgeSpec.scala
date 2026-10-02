package io.github.stivens.forge

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.testsetup.IOEffect
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

object RepositoryTemplate extends Forge with IOEffect

class ForgeSpec extends AnyFunSpec {
  describe("given a repository built from a Forge") {
    case class User(id: Long, name: String) derives Read, Write
    case class UpdateUser(name: Option[String] = None)

    object UserRepository
        extends RepositoryTemplate.Simple[User](tableName = "users_repository_template")
        with RepositoryTemplate.IdentifiedBy[User, Long](_.id)
        with RepositoryTemplate.Updates[User, Long, UpdateUser](
          handleUpdate = toFragments[UpdateUser].usingNonEmpty(_.name)(name => fr"name = $name").compile
        )
        with RepositoryTemplate.Deletions[User, Long] {

      def renameOrFail(id: Long, name: String): IO[Option[User]] =
        transact(getByIdOrFailC(id).flatMap(user => updateC(user.id, UpdateUser(name = Some(name)))))
    }

    sql"""
        DROP TABLE IF EXISTS users_repository_template;
        CREATE TABLE users_repository_template (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL);
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val users = List(User(id = 1, name = "John"), User(id = 2, name = "Jane"))
    UserRepository.createMany(users).unsafeRunSync()

    it("should return the forge's effect from public methods") {
      val getById: IO[Option[User]] = UserRepository.getById(1L)
      assert(getById.unsafeRunSync() == Some(users.head))
      assert(UserRepository.renameOrFail(2L, "Janet").unsafeRunSync() == Some(User(2, "Janet")))
      assert(UserRepository.delete(1L).unsafeRunSync() == Some(users.head))
    }

    it("should conform to the forge's interface aliases") {
      val _: RepositoryTemplate.GetByIdOps[User, Long] & RepositoryTemplate.DeleteOps[User, Long]     = UserRepository
      val _: RepositoryTemplate.CanGetById[User, Long] & RepositoryTemplate.CanDeleteMany[User, Long] = UserRepository
      val _: interface.GetByIdOps.Of[IO, User, Long]                                                  = UserRepository
    }

    it("should accept hand-written implementations of the forge's interface aliases") {
      val mock: RepositoryTemplate.GetAllOps[User] = new interface.GetAllOps.Generic[User] with RepositoryTemplate.Bound {
        override def getAll: IO[List[User]] = IO.pure(users)
        override def countAll: IO[Int]      = IO.pure(users.size)
      }
      assert(mock.countAll.unsafeRunSync() == 2)

      val singleMethodMock: RepositoryTemplate.CanGetById[User, Long] = new interface.CanGetById.Generic[User, Long]
        with RepositoryTemplate.Bound {
        override def getById(id: Long): IO[Option[User]] = IO.pure(users.find(_.id == id))
      }
      assert(singleMethodMock.getById(2L).unsafeRunSync() == Some(users(1)))
    }
  }

  describe("given a view built from a Forge") {
    case class Movie(id: Long, title: String)
    case class DbMovie(id: Long, movie_title: String) derives Read

    object MovieView
        extends RepositoryTemplate.View.WithIntermediateType[Movie, DbMovie](
          tableName = "movies_repository_template",
          dbToEntity = db => Movie(db.id, db.movie_title)
        )

    sql"""
        DROP TABLE IF EXISTS movies_repository_template;
        CREATE TABLE movies_repository_template (id SERIAL PRIMARY KEY, movie_title VARCHAR(255) NOT NULL);
        INSERT INTO movies_repository_template (movie_title) VALUES ('Alien'), ('Heat');
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    it("should map rows and return the forge's effect") {
      val all: IO[List[Movie]] = MovieView.getAll
      assert(all.unsafeRunSync().sortBy(_.id) == List(Movie(1, "Alien"), Movie(2, "Heat")))
    }
  }

  describe("given a forge's effect") {
    case class Thing(id: Long) derives Read, Write

    it("should not accept a second effect") {
      assertTypeError("""object Things extends RepositoryTemplate.Simple[Thing]("things") with ConnectionIOBinding""")
    }

    it("should not accept ConnectionIO mixins") {
      assertTypeError("""object Things extends RepositoryTemplate.Simple[Thing]("things") with mixins.IdentifiedBy[Thing, Long](_.id)""")
    }
  }
}
