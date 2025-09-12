package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.Fragments.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.github.stivens.forge.*
import io.github.stivens.forge.interface.FSPOps.*
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

import java.time.LocalDate

class FSPSpec extends AnyFunSpec {
  describe("given view with FSP mixin") {
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

    type MovieCursor = Movie

    enum MovieOrder {
      case ID, NAME, DIRECTOR, RELEASE_DATE, RATING
    }

    object MovieOrder {
      given OrderDefaultValue[MovieOrder] = new OrderDefaultValue[MovieOrder] {
        override def get: MovieOrder = MovieOrder.ID
      }
    }

    object MovieRepository
        extends AbstractRepository.Simple[Movie](fr"movies_fsp")
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
        with FSP[Movie, Movie, MovieFilter, MovieOrder, MovieCursor](
          evalOrder = sortDefinition =>
            comma(
              (sortDefinition.by match {
                case MovieOrder.ID           => NonEmptyList.of(fr"id")
                case MovieOrder.NAME         => NonEmptyList.of(fr"name", fr"id")
                case MovieOrder.DIRECTOR     => NonEmptyList.of(fr"director", fr"id")
                case MovieOrder.RELEASE_DATE => NonEmptyList.of(fr"releaseDate", fr"id")
                case MovieOrder.RATING       => NonEmptyList.of(fr"rating", fr"id")
              }).map(_ ++ sortDefinition.frAscOrDesc)
            ),
          evalCursor = (cursor, sortDefinition) =>
            sortDefinition.by match {
              case MovieOrder.ID           => fr"id ${sortDefinition.frSortSign} ${cursor.id}"
              case MovieOrder.NAME         => fr"(name, id) ${sortDefinition.frSortSign} (${cursor.name}, ${cursor.id})"
              case MovieOrder.DIRECTOR     => fr"(director, id) ${sortDefinition.frSortSign} (${cursor.director}, ${cursor.id})"
              case MovieOrder.RELEASE_DATE => fr"(releaseDate, id) ${sortDefinition.frSortSign} (${cursor.releaseDate}, ${cursor.id})"
              case MovieOrder.RATING       => fr"(rating, id) ${sortDefinition.frSortSign} (${cursor.rating}, ${cursor.id})"
            },
          constructCursor = movie => movie
        )

    // init relation
    sql"""
        DROP TABLE IF EXISTS movies_fsp;
        CREATE TABLE movies_fsp (
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

    it("should filter entities") {
      val fspRequest = FSPRequest(pageSize = Some(999), filter = Some(MovieFilter(director_eq = Some("Director 1"))))
      val result     = MovieRepository.fsp(fspRequest).transact(transactor).unsafeRunSync()

      assert(result.entities.size == 4)
      assert(result == FSPResponse(entities = movies.filter(_.director == "Director 1"), hasNextPage = false, nextPageCursor = None))
    }

    it("should paginate") {
      val fspRequestPage1 = FSPRequest(
        pageSize = Some(2),
        sort = Some(SortDefinition(by = MovieOrder.ID))
      )

      val resultPage1 = MovieRepository.fsp(fspRequestPage1).transact(transactor).unsafeRunSync()

      val fspRequestPage2 = fspRequestPage1.copy(after = Some(resultPage1.nextPageCursor.get))

      val resultPage2 = MovieRepository.fsp(fspRequestPage2).transact(transactor).unsafeRunSync()

      val fspRequestPage3 = fspRequestPage2.copy(after = Some(resultPage2.nextPageCursor.get), pageSize = Some(999))

      val resultPage3 = MovieRepository.fsp(fspRequestPage3).transact(transactor).unsafeRunSync()

      assert(resultPage1.entities.size == 2)
      assert(resultPage1.entities.map(_.id) == List(1, 2))
      assert(resultPage1.hasNextPage)

      assert(resultPage2.entities.size == 2)
      assert(resultPage2.hasNextPage)
      assert(resultPage2.entities.map(_.id) == List(3, 4))

      assert(resultPage3.entities.size == 6)
      assert(!resultPage3.hasNextPage)
      assert(resultPage3.entities.map(_.id) == List(5, 6, 7, 8, 9, 10))
    }

    it("should sort (asc)") {
      val fspRequestSortId = FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.ID, ascOrDesc = AscOrDesc.ASC)))
      val fspRequestSortName =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.NAME, ascOrDesc = AscOrDesc.ASC)))
      val fspRequestSortDirector =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.DIRECTOR, ascOrDesc = AscOrDesc.ASC)))
      val fspRequestSortReleaseDate =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.RELEASE_DATE, ascOrDesc = AscOrDesc.ASC)))
      val fspRequestSortRating =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.RATING, ascOrDesc = AscOrDesc.ASC)))

      val resultSortId          = MovieRepository.fsp(fspRequestSortId).transact(transactor).unsafeRunSync()
      val resultSortName        = MovieRepository.fsp(fspRequestSortName).transact(transactor).unsafeRunSync()
      val resultSortDirector    = MovieRepository.fsp(fspRequestSortDirector).transact(transactor).unsafeRunSync()
      val resultSortReleaseDate = MovieRepository.fsp(fspRequestSortReleaseDate).transact(transactor).unsafeRunSync()
      val resultSortRating      = MovieRepository.fsp(fspRequestSortRating).transact(transactor).unsafeRunSync()

      assert(resultSortId.entities == movies.sortBy(_.id))
      assert(resultSortName.entities == movies.sortBy(_.name))
      assert(resultSortDirector.entities == movies.sortBy(_.director))
      assert(resultSortReleaseDate.entities == movies.sortBy(_.releaseDate))
      assert(resultSortRating.entities == movies.sortBy(_.rating))
    }

    it("should sort (desc)") {
      val fspRequestSortId =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.ID, ascOrDesc = AscOrDesc.DESC)))
      val fspRequestSortName =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.NAME, ascOrDesc = AscOrDesc.DESC)))
      val fspRequestSortDirector =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.DIRECTOR, ascOrDesc = AscOrDesc.DESC)))
      val fspRequestSortReleaseDate =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.RELEASE_DATE, ascOrDesc = AscOrDesc.DESC)))
      val fspRequestSortRating =
        FSPRequest(pageSize = Some(999), sort = Some(SortDefinition(by = MovieOrder.RATING, ascOrDesc = AscOrDesc.DESC)))

      val resultSortId          = MovieRepository.fsp(fspRequestSortId).transact(transactor).unsafeRunSync()
      val resultSortName        = MovieRepository.fsp(fspRequestSortName).transact(transactor).unsafeRunSync()
      val resultSortDirector    = MovieRepository.fsp(fspRequestSortDirector).transact(transactor).unsafeRunSync()
      val resultSortReleaseDate = MovieRepository.fsp(fspRequestSortReleaseDate).transact(transactor).unsafeRunSync()
      val resultSortRating      = MovieRepository.fsp(fspRequestSortRating).transact(transactor).unsafeRunSync()

      assert(resultSortId.entities == movies.sortBy(_.id).reverse)
      assert(resultSortName.entities == movies.sortBy(_.name).reverse)
      assert(resultSortDirector.entities == movies.sortBy(_.director).reverse)
      assert(resultSortReleaseDate.entities == movies.sortBy(_.releaseDate).reverse)
      assert(resultSortRating.entities == movies.sortBy(_.rating).reverse)
    }
  }
}
