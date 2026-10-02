package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import doobie.*
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractView
import io.github.stivens.forge.interface.*
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty
import io.github.stivens.forge.util.CollectionUtil.orNotFound
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
  */
trait IdentifiedBy[Entity, ID](
    protected val extractId: Entity => ID,
    protected val frId: Fragment = fr"id"
)(using
    protected val _writeId: Write[ID]
) extends GetByIdOps[Entity, ID]
    with IdentifiedBy.Core[Entity, ID] {
  this: AbstractView[Entity, ?] =>
}

object IdentifiedBy {

  trait Generic[Entity, ID](
      protected val extractId: Entity => ID,
      protected val frId: Fragment = fr"id"
  )(using
      protected val _writeId: Write[ID]
  ) extends Core[Entity, ID] {
    this: AbstractView[Entity, ?] =>
  }

  trait Core[Entity, ID] extends GetByIdOps.Generic[Entity, ID] {
    this: AbstractView[Entity, ?] =>

    protected val extractId: Entity => ID
    protected val frId: Fragment
    protected given _writeId: Write[ID]

    final override def getManyByIds(ids: NonEmptyList[ID]): Eff[List[Entity]] = transact(getManyByIdsC(ids))
    final override def getManyByIds(ids: List[ID]): Eff[List[Entity]]         = transact(getManyByIdsC(ids))
    final def getManyByIdsToMap(ids: List[ID]): Eff[Map[ID, Entity]]          = transact(getManyByIdsToMapC(ids))
    final override def getById(id: ID): Eff[Option[Entity]]                   = transact(getByIdC(id))
    final override def getByIdOrFail(id: ID): Eff[Entity]                     = transact(getByIdOrFailC(id))

    final protected def getManyByIdsC(ids: NonEmptyList[ID]): ConnectionIO[List[Entity]] =
      runSelect(frSelectColumnsFromTable ++ whereAnd(frIdsIn(ids)))

    final protected def getManyByIdsC(ids: List[ID]): ConnectionIO[List[Entity]] =
      ids.mapNelOrEmpty(getManyByIdsC(_))

    final protected def getManyByIdsToMapC(ids: List[ID]): ConnectionIO[Map[ID, Entity]] =
      getManyByIdsC(ids).map(_.toMapBy(extractId))

    final protected def getByIdC(id: ID): ConnectionIO[Option[Entity]] =
      getManyByIdsC(NonEmptyList.one(id)).map(_.headOption)

    final protected def getByIdOrFailC(id: ID): ConnectionIO[Entity] =
      getByIdC(id).orNotFound(id)

    protected def frIdsIn(ids: NonEmptyList[ID]): Fragment = in(frId, ids)

    final protected type _ID = ID
  }
}
