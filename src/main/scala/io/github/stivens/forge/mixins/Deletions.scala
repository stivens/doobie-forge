package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import doobie.*
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.interface.*

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
trait Deletions[Entity, EntityId] extends DeleteOps[Entity, EntityId] {
  this: AbstractRepository[Entity, ?] & IdentifiedBy[Entity, EntityId] =>

  final def deleteMany(ids: NonEmptyList[EntityId]): ConnectionIO[List[Entity]] =
    runUpdateMany(fr"DELETE FROM $frTableName" ++ whereAnd(frIdsIn(ids)) ++ frWithReturning)
}
