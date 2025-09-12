package io.github.stivens.forge.interface

import cats.*
import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.free.connection
import doobie.implicits.*
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.mixins.*
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

class DependencyInjectionExample extends AnyFunSpec {
  describe("given simple dependency injection example") {
    case class Movie(id: Long, name: String, director: String, rating: Double)
    case class DirectorAverageRating(director: String, averageRating: Double)

    class DirectorAverageRatingRefresherService(
        movieRepository: GetAllOps[Movie],
        directorAverageRatingRepository: UpsertOps[DirectorAverageRating]
    ) {
      def refresh(): List[DirectorAverageRating] = (for {
        movies <- movieRepository.getAll
        directorAverageRatings = calculateDirectorAverageRating(movies)
        _ <- directorAverageRatingRepository.upsertMany(directorAverageRatings)
      } yield directorAverageRatings).transact(transactor).unsafeRunSync()

      private def calculateDirectorAverageRating(movies: List[Movie]): List[DirectorAverageRating] =
        movies
          .groupBy(_.director)
          .map { case (director, movies) =>
            DirectorAverageRating(director, movies.map(_.rating).sum / movies.size)
          }
          .toList
    }

    describe("conrecte repositories") {
      object MovieRepository extends AbstractRepository.Simple[Movie](fr"movies")
      object DirectorAverageRatingRepository
          extends AbstractRepository.Simple[DirectorAverageRating](fr"director_average_ratings")
          with IdentifiedBy[DirectorAverageRating, String](extractId = _.director, frId = fr"director")
          with Upsertions[DirectorAverageRating]

      it("should be subtypes of the required interfaces") {
        assert(MovieRepository.isInstanceOf[GetAllOps[Movie]])
        assert(DirectorAverageRatingRepository.isInstanceOf[UpsertOps[DirectorAverageRating]])

        assert {
          DirectorAverageRatingRepository.isInstanceOf[
            GetAllOps[DirectorAverageRating] & CreateOps[DirectorAverageRating] & UpsertOps[DirectorAverageRating] &
              GetByIdOps[DirectorAverageRating, String]
          ]
        }

      }

      it("should be injectable") {
        val DirectorAverageRatingRefresherService =
          new DirectorAverageRatingRefresherService(MovieRepository, DirectorAverageRatingRepository)
        assert(true) // it compiles
      }
    }

    describe("given mock instances") {
      val upsertRequestsLog = scala.collection.mutable.ListBuffer[DirectorAverageRating]()

      val directorAverageRatingRefresherService = new DirectorAverageRatingRefresherService(
        movieRepository = new GetAllOps[Movie] {
          val movies = List(
            Movie(1, "Movie 1", "Director 1", 5.0),
            Movie(2, "Movie 2", "Director 1", 4.0),
            Movie(3, "Movie 3", "Director 2", 3.0)
          )
          override def getAll: ConnectionIO[List[Movie]] = connection.pure(movies)
          override def countAll: ConnectionIO[Int]       = connection.pure(movies.size)
        },
        directorAverageRatingRepository = new UpsertOps[DirectorAverageRating] {
          override def upsertMany(entities: NonEmptyList[DirectorAverageRating]): ConnectionIO[List[DirectorAverageRating]] = {
            upsertRequestsLog.addAll(entities.toList)
            connection.pure(entities.toList)
          }
        }
      )

      it("we should be able to test the service") {
        val returnedValue  = directorAverageRatingRefresherService.refresh().sortBy(_.director)
        val upsertRequests = upsertRequestsLog.toList.sortBy(_.director)

        val expectedREsult = List(
          DirectorAverageRating("Director 1", 4.5),
          DirectorAverageRating("Director 2", 3.0)
        )

        assert(returnedValue == expectedREsult)
        assert(upsertRequests == expectedREsult)
      }
    }
  }
}
