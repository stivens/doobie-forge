package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import doobie.*
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.AsFragments
import io.github.stivens.forge.interface.UpdateOps
import io.github.stivens.forge.util.CollectionUtil.mapNelOrSucceedWith

/** A mixin trait that provides update capabilities for database repositories.
  *
  * This trait enables repositories to update entities by their IDs, returning the updated
  * entities. It automatically implements the `UpdateOps` interface, providing methods
  * for single and batch updates using a flexible update type system.
  *
  * @tparam Entity The type of entity being updated
  * @tparam ID The type of the entity identifier
  * @tparam UpdateType The type representing the update data (must be a Product type)
  * @param handleUpdate An `AsFragments[UpdateType]` instance that converts update data to SQL fragments
  *
  * @example {{{
  *   case class User(id: Long, name: String, email: String) derives Read, Write
  *   case class UpdateUser(name: Option[String] = None, email: Option[String] = None)
  *
  *   object UserRepository extends AbstractRepository.Simple[User](fr"users")
  *     with IdentifiedBy[User, Long](_.id)
  *     with Updates[User, Long, UpdateUser](
  *       handleUpdate = toFragments[UpdateUser]
  *         .usingNonEmpty(_.name)(name => fr"name = ${name}")
  *         .usingNonEmpty(_.email)(email => fr"email = ${email}")
  *         .compile
  *     )
  *
  *   // Now you can use:
  *   val update = UpdateUser(name = Some("John Doe"))
  *   val updatedUser: Option[User] = UserRepository.update(1L, update).transact(transactor).unsafeRunSync()
  *   val updatedUsers: List[User] = UserRepository.updateMany(List(1L, 2L), update).transact(transactor).unsafeRunSync()
  * }}}
  *
  * @note This trait requires the implementing class to extend both `AbstractRepository[Entity, ?]` and `IdentifiedBy[Entity, ID]`
  */
trait Updates[Entity, ID, UpdateType <: Product](
    protected val handleUpdate: AsFragments[UpdateType]
) extends UpdateOps[Entity, ID, UpdateType] {
  this: AbstractRepository[Entity, ?] & IdentifiedBy[Entity, ID] =>

  private given Write[_ID] = _writeId

  protected def frUpdateTable: Fragment = fr"UPDATE $frTableName"

  final def updateMany(ids: NonEmptyList[ID], update: UpdateType): ConnectionIO[List[Entity]] =
    evalUpdate(update).mapNelOrSucceedWith(
      updateFragments =>
        runUpdateMany {
          frUpdateTable ++ set(updateFragments) ++ whereAnd(in(frId, ids)) ++ frWithReturning
        },
      default = List.empty
    )

  private def evalUpdate(update: UpdateType): List[Fragment] =
    handleUpdate.eval(update).flatten
}
