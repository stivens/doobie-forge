package io.github.stivens.forge

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

import java.time.LocalDate

class AbstractRepositorySpec extends AnyFunSpec {
  describe("given simple repository") {
    case class Movie(
        id: Long,
        name: String,
        rating: Double,
        director: String,
        releaseDate: Option[LocalDate]
    ) derives Read,
          Write

    object MovieRepository extends AbstractRepository.Simple[Movie](tableName = fr"movies")

    // init relation
    sql"""
        DROP TABLE IF EXISTS movies;
        CREATE TABLE movies (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            rating DECIMAL(10, 2) NOT NULL,
            director VARCHAR(255) NOT NULL,
            releasedate DATE NULL
        );
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val movies = List(
      Movie(id = 1, name = "Movie 1", rating = 5.0, director = "Director 1", releaseDate = None),
      Movie(id = 2, name = "Movie 2", rating = 4.0, director = "Director 2", releaseDate = None),
      Movie(id = 3, name = "Movie 3", rating = 3.0, director = "Director 3", releaseDate = None),
      Movie(id = 4, name = "Movie 4", rating = 2.0, director = "Director 4", releaseDate = None)
    )

    it("should create and get entities") {
      val createResult   = MovieRepository.createMany(movies).transact(transactor).unsafeRunSync()
      val getAllResult   = MovieRepository.getAll.transact(transactor).unsafeRunSync()
      val countAllResult = MovieRepository.countAll.transact(transactor).unsafeRunSync()

      assert(createResult == movies)
      assert(getAllResult == movies)
      assert(countAllResult == movies.size)
    }
  }

  describe("given repository with intermediate type") {
    case class Address(street: String, city: String, state: String, zip: String)
    case class Person(id: Long, firstName: String, lastName: String, address: Address)

    case class DbPerson(
        id: Long,
        first_name: String,
        last_name: String,
        address_street: String,
        address_city: String,
        address_state: String,
        address_zip: String
    ) derives Read,
          Write

    object PersonRepository
        extends AbstractRepository.WithIntermediateType[Person, DbPerson](
          tableName = fr"people",
          dbMapping = new DbMapping[Person, DbPerson] {
            def dbToEntity(db: DbPerson): Person =
              Person(
                id = db.id,
                firstName = db.first_name,
                lastName = db.last_name,
                address = Address(street = db.address_street, city = db.address_city, state = db.address_state, zip = db.address_zip)
              )
            def entityToDb(entity: Person): DbPerson =
              DbPerson(
                id = entity.id,
                first_name = entity.firstName,
                last_name = entity.lastName,
                address_street = entity.address.street,
                address_city = entity.address.city,
                address_state = entity.address.state,
                address_zip = entity.address.zip
              )
          }
        )

    // init relation
    sql"""
        DROP TABLE IF EXISTS people;
        CREATE TABLE people (
            id SERIAL PRIMARY KEY,
            first_name VARCHAR(255) NOT NULL,
            last_name VARCHAR(255) NOT NULL,
            address_street VARCHAR(255) NOT NULL,
            address_city VARCHAR(255) NOT NULL,
            address_state VARCHAR(255) NOT NULL,
            address_zip VARCHAR(255) NOT NULL
        );
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val people = List(
      Person(
        id = 1,
        firstName = "John",
        lastName = "Doe",
        address = Address(street = "123 Main St", city = "Anytown", state = "CA", zip = "12345")
      ),
      Person(
        id = 2,
        firstName = "Jane",
        lastName = "Doe",
        address = Address(street = "456 Main St", city = "Anytown", state = "CA", zip = "12345")
      ),
      Person(
        id = 3,
        firstName = "Jim",
        lastName = "Beam",
        address = Address(street = "789 Main St", city = "Anytown", state = "CA", zip = "12345")
      ),
      Person(
        id = 4,
        firstName = "Jill",
        lastName = "Bean",
        address = Address(street = "101 Main St", city = "Anytown", state = "CA", zip = "12345")
      )
    )

    it("should create and get entities") {
      val createResult   = PersonRepository.createMany(people).transact(transactor).unsafeRunSync()
      val getAllResult   = PersonRepository.getAll.transact(transactor).unsafeRunSync()
      val countAllResult = PersonRepository.countAll.transact(transactor).unsafeRunSync()

      assert(createResult == people)
      assert(getAllResult == people)
      assert(countAllResult == people.size)
    }
  }

  describe("given repository with column order different from the case class") {
    case class Movie(
        id: Long,
        name: String,
        rating: Double,
        director: String,
        releaseDate: Option[LocalDate]
    ) derives Read,
          Write

    object MovieRepository extends AbstractRepository.Simple[Movie](tableName = fr"movies_unordered")

    // init relation
    sql"""
        DROP TABLE IF EXISTS movies_unordered;
        CREATE TABLE movies_unordered (
            releasedate DATE NULL,
            id SERIAL PRIMARY KEY,
            director VARCHAR(255) NOT NULL,
            name VARCHAR(255) NOT NULL,
            rating DECIMAL(10, 2) NOT NULL
        );
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val movies = List(
      Movie(id = 1, name = "Movie 1", rating = 5.0, director = "Director 1", releaseDate = None),
      Movie(id = 2, name = "Movie 2", rating = 4.0, director = "Director 2", releaseDate = None),
      Movie(id = 3, name = "Movie 3", rating = 3.0, director = "Director 3", releaseDate = None),
      Movie(id = 4, name = "Movie 4", rating = 2.0, director = "Director 4", releaseDate = None)
    )

    it("should create and get entities") {
      val createResult = MovieRepository.createMany(movies).transact(transactor).unsafeRunSync()
      val getAllResult = MovieRepository.getAll.transact(transactor).unsafeRunSync()

      assert(createResult == movies)
      assert(getAllResult == movies)
    }
  }
}
