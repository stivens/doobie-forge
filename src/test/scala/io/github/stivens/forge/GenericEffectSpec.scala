package io.github.stivens.forge

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.interface.*
import io.github.stivens.forge.mixins.*
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

trait IOEffect extends EffectLift {
  final type Eff[A] = IO[A]
  final protected def lift[A](io: ConnectionIO[A]): IO[A] = io.transact(transactor)
}

class GenericEffectSpec extends AnyFunSpec {
  describe("given a Generic repository with a custom effect") {
    case class User(id: Long, name: String, email: Option[String]) derives Read, Write
    case class UserFilter(name_eq: Option[String] = None)
    case class UpdateUser(name: Option[String] = None)

    object UserRepository
        extends AbstractRepository.Generic[User](tableName = "users_generic_effect")
        with IOEffect
        with IdentifiedBy[User, Long](_.id)
        with Filtering[User, UserFilter](
          handleFilter = toFragments[UserFilter].usingNonEmpty(_.name_eq)(name => fr"name = $name").compile
        )
        with Updates[User, Long, UpdateUser](
          handleUpdate = toFragments[UpdateUser].usingNonEmpty(_.name)(name => fr"name = $name").compile
        )
        with Upsertions[User]
        with Deletions[User, Long] {

      def getAllWithoutEmail: IO[List[User]] = lift(selectWith(fr"WHERE email IS NULL"))

      def renameOrFail(id: Long, name: String): IO[Option[User]] =
        lift(getByIdOrFailC(id).flatMap(user => updateC(user.id, UpdateUser(name = Some(name)))))
    }

    sql"""
        DROP TABLE IF EXISTS users_generic_effect;
        CREATE TABLE users_generic_effect (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255));
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val users = List(
      User(id = 1, name = "John", email = Some("john@example.com")),
      User(id = 2, name = "Jane", email = None),
      User(id = 3, name = "Jim", email = None)
    )

    UserRepository.createMany(users).unsafeRunSync()

    it("should return the custom effect from public methods") {
      val getById: IO[Option[User]] = UserRepository.getById(1L)
      assert(getById.unsafeRunSync() == Some(users.head))
      assert(UserRepository.getAll.unsafeRunSync().sortBy(_.id) == users)
      assert(UserRepository.countByFilter(UserFilter(name_eq = Some("Jane"))).unsafeRunSync() == 1)
    }

    it("should let custom methods lift protected helpers and ConnectionIO twins") {
      assert(UserRepository.getAllWithoutEmail.unsafeRunSync().map(_.id).sorted == List(2L, 3L))
      assert(UserRepository.renameOrFail(3L, "James").unsafeRunSync() == Some(users(2).copy(name = "James")))
      assertThrows[EntityNotFoundError[?, ?]](UserRepository.renameOrFail(99L, "Nobody").unsafeRunSync())
    }

    it("should upsert and delete through the custom effect") {
      val upserted = User(id = 2, name = "Jane", email = Some("jane@example.com"))
      assert(UserRepository.upsert(upserted).unsafeRunSync() == upserted)
      assert(UserRepository.delete(1L).unsafeRunSync() == Some(users.head))
      assert(UserRepository.getById(1L).unsafeRunSync() == None)
    }

    it("should conform to the interfaces refined with the custom effect") {
      val _: GetByIdOps.Of[IO, User, Long] & UpsertOps.Of[IO, User] & DeleteOps.Of[IO, User, Long] = UserRepository
      assertTypeError("val _: GetAllOps.Of[ConnectionIO, User] = UserRepository")
    }
  }

  describe("given a Generic.WithIntermediateType view with a custom effect") {
    case class Movie(id: Long, title: String)
    case class DbMovie(id: Long, movie_title: String) derives Read, Write

    object MovieView
        extends AbstractView.Generic.WithIntermediateType[Movie, DbMovie](
          tableName = "movies_generic_effect",
          dbToEntity = db => Movie(db.id, db.movie_title)
        )
        with IOEffect

    sql"""
        DROP TABLE IF EXISTS movies_generic_effect;
        CREATE TABLE movies_generic_effect (id SERIAL PRIMARY KEY, movie_title VARCHAR(255) NOT NULL);
        INSERT INTO movies_generic_effect (movie_title) VALUES ('Alien'), ('Heat');
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    it("should map rows and return the custom effect") {
      val all: IO[List[Movie]] = MovieView.getAll
      assert(all.unsafeRunSync().sortBy(_.id) == List(Movie(1, "Alien"), Movie(2, "Heat")))
      assert(MovieView.countAll.unsafeRunSync() == 2)
    }
  }

  describe("given a ConnectionIO repository") {
    it("should not accept a second effect") {
      case class Thing(id: Long) derives Read, Write
      assertTypeError("""object Things extends AbstractRepository.Simple[Thing]("things") with IOEffect""")
    }
  }
}
