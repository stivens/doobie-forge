package io.github.stivens.forge.mixins

import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.github.stivens.forge.*
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

import java.time.LocalDate

class FilteringSpec extends AnyFunSpec {
  describe("given view with Filtering mixin") {
    case class Movie(
        id: Long,
        name: String,
        director: String,
        releaseDate: LocalDate,
        rating: Double
    ) derives Read,
          Write

    case class MovieFilter(
        name_like: Option[String] = None,
        director_eq: Option[String] = None,
        releaseDate_gte: Option[LocalDate] = None,
        releaseDate_lte: Option[LocalDate] = None,
        rating_gte: Option[Double] = None,
        rating_lte: Option[Double] = None
    )

    object MovieRepository
        extends AbstractRepository.Simple[Movie](tableName = "movies_to_be_filtered")
        with Filtering[Movie, MovieFilter](handleFilter =
          toFragments[MovieFilter]
            .usingNonEmpty(_.name_like)(name => fr"name LIKE ${%%(name)}")
            .usingNonEmpty(_.director_eq)(director => fr"director = ${director}")
            .usingNonEmpty(_.releaseDate_gte)(releaseDate => fr"releaseDate >= ${releaseDate}")
            .usingNonEmpty(_.releaseDate_lte)(releaseDate => fr"releaseDate <= ${releaseDate}")
            .usingNonEmpty(_.rating_gte)(rating => fr"rating >= ${rating}")
            .usingNonEmpty(_.rating_lte)(rating => fr"rating <= ${rating}")
            .compile
        )

    // init relation
    sql"""
        DROP TABLE IF EXISTS movies_to_be_filtered;
        CREATE TABLE movies_to_be_filtered (
            id SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            director VARCHAR(255) NOT NULL,
            releaseDate DATE NOT NULL,
            rating DECIMAL(10, 2) NOT NULL
        );
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val movies = List(
      Movie(id = 1, name = "Movie 1", director = "Director 1", releaseDate = LocalDate.of(2024, 1, 1), rating = 5.0),
      Movie(id = 2, name = "Movie 2", director = "Director 2", releaseDate = LocalDate.of(2024, 1, 2), rating = 4.0),
      Movie(id = 3, name = "Movie 3", director = "Director 3", releaseDate = LocalDate.of(2024, 1, 3), rating = 3.0),
      Movie(id = 4, name = "Movie 4", director = "Director 1", releaseDate = LocalDate.of(2024, 1, 4), rating = 2.0),
      Movie(id = 5, name = "Movie 5", director = "Director 2", releaseDate = LocalDate.of(2024, 1, 5), rating = 1.0),
      Movie(id = 6, name = "Movie 6", director = "Director 3", releaseDate = LocalDate.of(2024, 1, 6), rating = 0.0),
      Movie(id = 7, name = "Movie 7", director = "Director 1", releaseDate = LocalDate.of(2024, 1, 7), rating = 4.5),
      Movie(id = 8, name = "Movie 8", director = "Director 2", releaseDate = LocalDate.of(2024, 1, 8), rating = 3.5),
      Movie(id = 9, name = "Movie 9", director = "Director 3", releaseDate = LocalDate.of(2024, 1, 9), rating = 4.0),
      Movie(id = 10, name = "Movie 10", director = "Director 1", releaseDate = LocalDate.of(2024, 1, 10), rating = 4.0)
    )

    MovieRepository.createMany(movies).transact(transactor).unsafeRunSync()

    it("should get many by filter") {
      val filterByName              = MovieFilter(name_like = Some("Movie 1"))
      val filterByDirector          = MovieFilter(director_eq = Some("Director 1"))
      val filterByDirectorAndRating = MovieFilter(director_eq = Some("Director 1"), rating_gte = Some(4.0))

      val filterByNameResult              = MovieRepository.getManyByFilter(filterByName).transact(transactor).unsafeRunSync()
      val filterByDirectorResult          = MovieRepository.getManyByFilter(filterByDirector).transact(transactor).unsafeRunSync()
      val filterByDirectorAndRatingResult = MovieRepository.getManyByFilter(filterByDirectorAndRating).transact(transactor).unsafeRunSync()

      val countByNameResult              = MovieRepository.countByFilter(filterByName).transact(transactor).unsafeRunSync()
      val countByDirectorResult          = MovieRepository.countByFilter(filterByDirector).transact(transactor).unsafeRunSync()
      val countByDirectorAndRatingResult = MovieRepository.countByFilter(filterByDirectorAndRating).transact(transactor).unsafeRunSync()

      assert(filterByNameResult.size == 2) // Movie 1 and Movie 10
      assert(countByNameResult == 2)
      assert(filterByNameResult == movies.filter(_.name.contains("Movie 1")))
      assert(filterByDirectorResult.size == 4)
      assert(countByDirectorResult == 4)
      assert(filterByDirectorResult == movies.filter(_.director == "Director 1"))
      assert(filterByDirectorAndRatingResult.size == 3)
      assert(countByDirectorAndRatingResult == 3)
      assert(filterByDirectorAndRatingResult == movies.filter(_.director == "Director 1").filter(_.rating >= 4.0))

    }
  }
}
