package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.Fragment
import doobie.free.connection
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.interface.UpsertOps

/** A mixin trait that provides upsert (insert or update) capabilities for database repositories.
  *
  * This trait enables repositories to perform upsert operations, which insert a new entity
  * if it doesn't exist or update an existing entity if it does. It automatically implements
  * the `UpsertOps` interface using PostgreSQL's `ON CONFLICT DO UPDATE` syntax.
  *
  * @tparam Entity The type of entity being upserted
  *
  * @example {{{
  *   case class User(id: Long, name: String, email: String) derives Read, Write
  *
  *   object UserRepository extends AbstractRepository.Simple[User](fr"users")
  *     with IdentifiedBy[User, Long](_.id)
  *     with Upsertions[User]
  *
  *   // Now you can use:
  *   val user = User(id = 1, name = "John Doe", email = "john.doe@example.com")
  *   val upsertedUser: User = UserRepository.upsert(user).transact(transactor).unsafeRunSync()
  * }}}
  *
  * @note This trait requires the implementing class to extend both `AbstractRepository[Entity, ?]` and `IdentifiedBy[Entity, ?]`
  */
trait Upsertions[Entity] extends UpsertOps[Entity] {
  this: AbstractRepository[Entity, ?] & IdentifiedBy[Entity, ?] =>

  // interface implementation

  final def upsert(entity: Entity): ConnectionIO[Entity] =
    createManyWithOnConflictDoHandle(NonEmptyList.one(entity))
      .map(_.headOption)
      .flatMap {
        case Some(entity) => connection.pure(entity)
        case None         => connection.raiseError(new IllegalStateException("Entity could not be fetched after its upsert"))
      }

  // internals

  protected val frUpsertValues: Fragment = columnsMeta.columnsListAsFragments
    .map(c => fr"$c = EXCLUDED.$c")
    .intercalate(fr",")

  override protected val frOnConflict: Fragment = fr"ON CONFLICT ($frId) DO UPDATE SET $frUpsertValues"
}
