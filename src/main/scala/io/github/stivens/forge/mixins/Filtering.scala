package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import doobie.ConnectionIO
import doobie.Fragment
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractView
import io.github.stivens.forge.AsFragments
import io.github.stivens.forge.interface.FilterOps

/** A mixin trait that provides filtering capabilities for database views.
  *
  * This trait enables views to perform filtered queries using flexible filter types.
  * It automatically implements the `FilterOps` interface, providing methods for
  * retrieving filtered entities and counting filtered results.
  *
  * @tparam Entity The type of entity being filtered
  * @tparam FilterType The type representing the filter criteria (must be a Product type)
  * @param handleFilter An `AsFragments[FilterType]` instance that converts filter data to SQL fragments
  *
  * @example {{{
  *   case class Movie(id: Long, name: String, director: String, rating: Double) derives Read, Write
  *   case class MovieFilter(
  *     name_like: Option[String] = None,
  *     director_eq: Option[String] = None,
  *     rating_gte: Option[Double] = None
  *   )
  *
  *   object MovieRepository extends AbstractView.Simple[Movie](fr"movies")
  *     with Filtering[Movie, MovieFilter](
  *       handleFilter = toFragments[MovieFilter]
  *         .usingNonEmpty(_.name_like)(name => fr"name LIKE ${%%(name)}")
  *         .usingNonEmpty(_.director_eq)(director => fr"director = ${director}")
  *         .usingNonEmpty(_.rating_gte)(rating => fr"rating >= ${rating}")
  *         .compile
  *     )
  *
  *   // Now you can use:
  *   val filter = MovieFilter(director_eq = Some("Director 1"), rating_gte = Some(4.0))
  *   val movies: List[Movie] = MovieRepository.getManyByFilter(filter).transact(transactor).unsafeRunSync()
  *   val count: Int = MovieRepository.countByFilter(filter).transact(transactor).unsafeRunSync()
  * }}}
  *
  * @note This trait requires the implementing class to extend `AbstractView[Entity, ?]`
  */
trait Filtering[Entity, FilterType <: Product](
    protected val handleFilter: AsFragments[FilterType]
) extends FilterOps[Entity, FilterType]
    with Filtering.Core[Entity, FilterType] {
  this: AbstractView[Entity, ?] =>
}

object Filtering {

  trait Generic[Entity, FilterType <: Product](
      protected val handleFilter: AsFragments[FilterType]
  ) extends Core[Entity, FilterType] {
    this: AbstractView[Entity, ?] =>
  }

  trait Core[Entity, FilterType <: Product] extends FilterOps.Generic[Entity, FilterType] {
    this: AbstractView[Entity, ?] =>

    protected val handleFilter: AsFragments[FilterType]

    final override def getManyByFilter(filter: FilterType): Eff[List[Entity]] = lift(getManyByFilterC(filter))
    final override def countByFilter(filter: FilterType): Eff[Int]            = lift(countByFilterC(filter))

    final protected def getManyByFilterC(filter: FilterType): ConnectionIO[List[Entity]] =
      selectWith(frWhereFilter(filter))

    final protected def countByFilterC(filter: FilterType): ConnectionIO[Int] =
      getCountWhere(frWhereFilter(filter))

    final protected def frWhereFilter(filter: FilterType): Fragment = {
      val filterConditions = toFilterConditions(filter)
      NonEmptyList.fromList(filterConditions).map(whereAnd).getOrElse(fr"WHERE 1=1")
    }

    final protected def toFilterConditions(filter: FilterType): List[Fragment] =
      handleFilter.eval(filter).flatten

    final protected type _FilterType = FilterType
  }
}
