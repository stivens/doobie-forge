package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import doobie.*
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.interface.*
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty

/** A mixin trait that provides deletion capabilities for database repositories.
  *
  * This trait enables repositories to delete entities by their IDs, returning the deleted
  * entities. It automatically implements the `DeleteOps` interface, providing methods
  * for single and batch deletions.
  *
  * @tparam Entity The type of entity being deleted
  * @tparam EntityId The type of the entity identifier
  *
  * @example {{{
  *   case class User(id: Long, name: String) derives Read, Write
  *
  *   object UserRepository extends AbstractRepository.Simple[User](fr"users")
  *     with IdentifiedBy[User, Long](_.id)
  *     with Deletions[User, Long]
  *
  *   // Now you can use:
  *   val deletedUser: Option[User] = UserRepository.delete(1L).transact(transactor).unsafeRunSync()
  *   val deletedUsers: List[User] = UserRepository.deleteMany(List(1L, 2L)).transact(transactor).unsafeRunSync()
  * }}}
  *
  * @note This trait requires the implementing class to extend both `AbstractRepository[Entity, ?]` and `IdentifiedBy[Entity, EntityId]`
  */
trait Deletions[Entity, EntityId] extends DeleteOps[Entity, EntityId] with Deletions.Core[Entity, EntityId] {
  this: AbstractRepository[Entity, ?] & IdentifiedBy.Core[Entity, EntityId] =>
}

object Deletions {

  trait Generic[Entity, EntityId] extends Core[Entity, EntityId] {
    this: AbstractRepository[Entity, ?] & IdentifiedBy.Core[Entity, EntityId] =>

    final override def deleteMany(ids: List[EntityId]): Eff[List[Entity]] = transact(deleteManyC(ids))
    final override def delete(id: EntityId): Eff[Option[Entity]]          = transact(deleteC(id))
  }

  trait Core[Entity, EntityId] extends DeleteOps.Generic[Entity, EntityId] {
    this: AbstractRepository[Entity, ?] & IdentifiedBy.Core[Entity, EntityId] =>

    final override def deleteMany(ids: NonEmptyList[EntityId]): Eff[List[Entity]] = transact(deleteManyC(ids))

    // not `frIdsIn`: `Joined` overrides it with the table alias, which DELETE doesn't declare
    final protected def deleteManyC(ids: NonEmptyList[EntityId]): ConnectionIO[List[Entity]] =
      runUpdateMany(fr"DELETE FROM $frTableName" ++ whereAnd(in(frId, ids)) ++ frWithReturning)

    final protected def deleteManyC(ids: List[EntityId]): ConnectionIO[List[Entity]] =
      ids.mapNelOrEmpty(deleteManyC(_))

    final protected def deleteC(id: EntityId): ConnectionIO[Option[Entity]] =
      deleteManyC(NonEmptyList.one(id)).map(_.headOption)
  }
}
