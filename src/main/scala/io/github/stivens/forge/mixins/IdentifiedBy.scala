package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import doobie.*
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractView
import io.github.stivens.forge.interface.*
import io.github.stivens.forge.util.CollectionUtil.toMapBy

/** A mixin trait that provides entity identification capabilities for database views.
  *
  * This trait enables database views to perform operations based on entity IDs, such as
  * retrieving entities by their identifiers. It automatically implements the `GetByIdOps`
  * interface, providing methods for single and batch ID-based lookups.
  *
  * @tparam Entity The type of entity being identified
  * @tparam ID The type of the identifier
  * @param extractId A function that extracts the ID from an entity instance
  * @param frId A SQL fragment representing the ID column name (defaults to "id")
  * @param writeId An implicit `Write[ID]` instance for SQL parameter binding
  *
  * @example {{{
  *   case class User(id: Long, name: String, email: String) derives Read, Write
  *
  *   object UserRepository extends AbstractRepository.Simple[User](fr"users") 
  *     with IdentifiedBy[User, Long](_.id)
  *
  *   // Now you can use:
  *   val user: Option[User] = UserRepository.getById(1L).transact(transactor).unsafeRunSync()
  *   val users: List[User] = UserRepository.getManyByIds(List(1L, 2L, 3L)).transact(transactor).unsafeRunSync()
  * }}}
  *
  * @note This trait requires the implementing class to extend `AbstractView[Entity, ?]`
  */
trait IdentifiedBy[Entity, ID](
    protected val extractId: Entity => ID,
    protected val frId: Fragment = fr"id"
)(using
    writeId: Write[ID]
) extends GetByIdOps[Entity, ID] {
  this: AbstractView[Entity, ?] =>

  // interface implementation

  /** Retrieves multiple entities by their IDs.
    *
    * @param ids A non-empty list of IDs to look up
    * @return A `ConnectionIO` that yields a list of entities matching the provided IDs
    */
  final override def getManyByIds(ids: NonEmptyList[ID]): ConnectionIO[List[Entity]] =
    runSelect(frSelectColumnsFromTable ++ whereAnd(frIdsIn(ids)))

  /** Retrieves multiple entities by their IDs and returns them as a map.
    *
    * @param ids A list of IDs to look up (can be empty)
    * @return A `ConnectionIO` that yields a map from ID to entity for all found entities
    */
  final def getManyByIdsToMap(ids: List[ID]): ConnectionIO[Map[ID, Entity]] =
    getManyByIds(ids).map(_.toMapBy(extractId))

  /** Retrieves a single entity by its ID.
    *
    * @param id The ID to look up
    * @return A `ConnectionIO` that yields an `Option[Entity]` - `Some(entity)` if found, `None` otherwise
    */
  final override def getById(id: ID): ConnectionIO[Option[Entity]] =
    getManyByIds(NonEmptyList.one(id)).map(_.headOption)

  // internals

  protected def frIdsIn(ids: NonEmptyList[ID]): Fragment = in(frId, ids)

  // Self-types and type aliases for convenience in subclasses

  final protected type _ID = ID
  final protected val _writeId = writeId
}
