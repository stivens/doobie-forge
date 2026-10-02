package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.Fragment
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.interface.UpsertOps
import io.github.stivens.forge.util.CollectionUtil.headOrFail

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
  */
trait Upsertions[Entity] extends UpsertOps[Entity] with Upsertions.Generic[Entity] {
  this: AbstractRepository[Entity, ?] & IdentifiedBy.Core[Entity, ?] =>
}

object Upsertions {

  trait Generic[Entity] extends UpsertOps.Generic[Entity] {
    this: AbstractRepository[Entity, ?] & IdentifiedBy.Core[Entity, ?] =>

    final override def upsertMany(entities: NonEmptyList[Entity]): Eff[List[Entity]] = transact(upsertManyC(entities))
    final override def upsertMany(entities: List[Entity]): Eff[List[Entity]]         = transact(upsertManyC(entities))
    final override def upsert(entity: Entity): Eff[Entity]                           = transact(upsertC(entity))

    final protected def upsertManyC(entities: NonEmptyList[Entity]): ConnectionIO[List[Entity]] =
      createManyWithOnConflictDoHandleC(entities)

    final protected def upsertManyC(entities: List[Entity]): ConnectionIO[List[Entity]] =
      createManyWithOnConflictDoHandleC(entities)

    final protected def upsertC(entity: Entity): ConnectionIO[Entity] =
      upsertManyC(NonEmptyList.one(entity)).headOrFail("upsert")

    protected val frUpsertValues: Fragment = columnsMeta.columnsListAsFragments
      .map(c => fr"$c = EXCLUDED.$c")
      .intercalate(fr",")

    override protected val frOnConflict: Fragment = fr"ON CONFLICT ON CONSTRAINT ${fr0TableName}_pkey DO UPDATE SET $frUpsertValues"
  }
}
