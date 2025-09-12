package io.github.stivens.forge.mixins

import doobie.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractView
import io.github.stivens.forge.DEFAUL_FSP_PAGE_SIZE
import io.github.stivens.forge.OrderDefaultValue
import io.github.stivens.forge.interface.FSPOps
import io.github.stivens.forge.interface.FSPOps.*
import io.github.stivens.forge.util.DoobieUtil.safeNumberConst0

/** A mixin trait that provides Filter-Sort-Paginate (FSP) capabilities for database views.
  *
  * This trait enables views to perform complex queries with filtering, sorting, and cursor-based
  * pagination. It automatically implements the `FSPOps` interface, providing methods for
  * paginated data retrieval and filtered counting.
  *
  * @tparam Entity The type of entity being queried
  * @tparam DbEntity The database entity type (must be a Product type)
  * @tparam FilterType The type representing filter criteria (must be a Product type)
  * @tparam Order The type representing sort order options
  * @tparam Cursor The type representing pagination cursor
  * @param evalOrder A function that converts sort definitions to SQL ORDER BY fragments
  * @param evalCursor A function that converts cursor and sort definition to SQL WHERE fragments for pagination
  * @param constructCursor A function that constructs a cursor from a database entity
  * @param orderDefaultValue An implicit `OrderDefaultValue[Order]` instance providing default sort order
  *
  * @example {{{
  *   import java.time.LocalDate
  *   case class Movie(id: Long, name: String, director: String, releaseDate: LocalDate, rating: Double) derives Read, Write
  *   case class MovieFilter(
  *     name_like: Option[String] = None,
  *     director_eq: Option[String] = None,
  *     releaseDate_gte: Option[LocalDate] = None,
  *     releaseDate_lte: Option[LocalDate] = None,
  *     rating_gte: Option[Double] = None,
  *     rating_lte: Option[Double] = None
  *   )
  *   type MovieCursor = Movie
  *   enum MovieOrder { case ID, NAME, DIRECTOR, RELEASE_DATE, RATING }
  *
  *   object MovieOrder {
  *     given OrderDefaultValue[MovieOrder] = new OrderDefaultValue[MovieOrder] {
  *       override def get: MovieOrder = MovieOrder.ID
  *     }
  *   }
  *
  *   object MovieRepository extends AbstractView.Simple[Movie](fr"movies")
  *     with Filtering[Movie, MovieFilter](handleFilter = toFragments[MovieFilter]
  *       .usingNonEmpty(_.name_like)(name => fr"name LIKE ${%%(name)}")
  *       .usingNonEmpty(_.director_eq)(director => fr"director = ${director}")
  *       .usingNonEmpty(_.releaseDate_gte)(releaseDate => fr"releaseDate >= ${releaseDate}")
  *       .usingNonEmpty(_.releaseDate_lte)(releaseDate => fr"releaseDate <= ${releaseDate}")
  *       .usingNonEmpty(_.rating_gte)(rating => fr"rating >= ${rating}")
  *       .usingNonEmpty(_.rating_lte)(rating => fr"rating <= ${rating}")
  *       .compile)
  *     with FSP[Movie, Movie, MovieFilter, MovieOrder, MovieCursor](
  *       evalOrder = sortDefinition =>
  *         comma(
  *           (sortDefinition.by match {
  *             case MovieOrder.ID           => NonEmptyList.of(fr"id")
  *             case MovieOrder.NAME         => NonEmptyList.of(fr"name", fr"id")
  *             case MovieOrder.DIRECTOR     => NonEmptyList.of(fr"director", fr"id")
  *             case MovieOrder.RELEASE_DATE => NonEmptyList.of(fr"releaseDate", fr"id")
  *             case MovieOrder.RATING       => NonEmptyList.of(fr"rating", fr"id")
  *           }).map(_ ++ sortDefinition.frAscOrDesc)
  *         ),
  *       evalCursor = (cursor, sortDefinition) =>
  *         sortDefinition.by match {
  *           case MovieOrder.ID           => fr"id ${sortDefinition.frSortSign} ${cursor.id}"
  *           case MovieOrder.NAME         => fr"(name, id) ${sortDefinition.frSortSign} (${cursor.name}, ${cursor.id})"
  *           case MovieOrder.DIRECTOR     => fr"(director, id) ${sortDefinition.frSortSign} (${cursor.director}, ${cursor.id})"
  *           case MovieOrder.RELEASE_DATE => fr"(releaseDate, id) ${sortDefinition.frSortSign} (${cursor.releaseDate}, ${cursor.id})"
  *           case MovieOrder.RATING       => fr"(rating, id) ${sortDefinition.frSortSign} (${cursor.rating}, ${cursor.id})"
  *         },
  *       constructCursor = movie => movie
  *     )
  *
  *   // Now you can use:
  *   val request = FSPRequest(
  *     pageSize = Some(10),
  *     filter = Some(MovieFilter(director_eq = Some("Director 1"), rating_gte = Some(4.0))),
  *     sort = Some(SortDefinition(by = MovieOrder.RATING, ascOrDesc = AscOrDesc.DESC))
  *   )
  *   val result: FSPResponse[Movie, MovieCursor] = MovieRepository.fsp(request).transact(transactor).unsafeRunSync()
  * }}}
  *
  * @note This trait requires the implementing class to extend both `AbstractView[Entity, DbEntity]` and `Filtering[Entity, FilterType]`
  */
trait FSP[Entity, DbEntity <: Product, FilterType <: Product, Order, Cursor](
    protected val evalOrder: SortDefinition[Order] => Fragment,
    protected val evalCursor: (Cursor, SortDefinition[Order]) => Fragment,
    protected val constructCursor: DbEntity => Cursor
)(using
    orderDefaultValue: OrderDefaultValue[Order]
) extends FSPOps[Entity, FilterType, Order, Cursor] {
  this: AbstractView[Entity, DbEntity] & Filtering[Entity, FilterType] =>

  /** Performs a Filter-Sort-Paginate query with the given request parameters.
    *
    * This method executes a complex query that combines filtering, sorting, and cursor-based
    * pagination..
    *
    * @param request The FSP request containing page size, cursor, filter, and sort parameters
    * @return A `ConnectionIO` that yields an `FSPResponse` with entities, pagination info, and next cursor
    */
  final def fsp(request: FSPRequestType): ConnectionIO[FSPResponseType] = {
    val pageSize = request.pageSize.getOrElse(DEFAUL_FSP_PAGE_SIZE)

    val sortDefinition = request.sort.getOrElse {
      SortDefinition(by = orderDefaultValue.get)
    }

    val frCursor = request.after.map(evalCursor(_, sortDefinition))

    val frOrderBy = fr"ORDER BY" ++ evalOrder(sortDefinition)

    val frLimit = fr"LIMIT ${safeNumberConst0(pageSize + 1)}" // fetch 1 more than requested to check if there is next page

    val frWhere = request.filter match {
      case Some(filter) => evalFilter(filter) ++ (frCursor.map(c => fr"AND $c").getOrElse(Fragment.empty))
      case None         => frCursor.map(c => fr"WHERE $c").getOrElse(Fragment.empty)
    }

    val sql = frSelectColumnsFromTable ++ frWhere ++ frOrderBy ++ frLimit

    for {
      dbEntitiesWithOneExtra <- sql.query[DbEntity](using _readDbEntity).to[List]
    } yield
      if (dbEntitiesWithOneExtra.length > pageSize /* == pageSize + 1 to be precise */ ) {
        val dbEntities = dbEntitiesWithOneExtra.init
        FSPResponse(
          entities = dbEntities.map(dbToEntity),
          hasNextPage = true,
          nextPageCursor = dbEntities.lastOption.map(constructCursor)
        )
      } else {
        FSPResponse(
          entities = dbEntitiesWithOneExtra.map(dbToEntity),
          hasNextPage = false,
          nextPageCursor = None
        )
      }
  }
}
