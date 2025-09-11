package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.Fragment
import doobie.free.connection
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.interface.UpsertOps

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
