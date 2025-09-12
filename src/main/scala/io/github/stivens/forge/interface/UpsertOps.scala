package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import doobie.free.connection
import io.github.stivens.forge.util.CollectionUtil.mapNelOrSucceedWith

trait UpsertOps[Entity] {
  def upsertMany(entities: NonEmptyList[Entity]): ConnectionIO[List[Entity]]

  final def upsertMany(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrSucceedWith(entitiesNel => upsertMany(entitiesNel), default = List.empty)

  final def upsert(entity: Entity): ConnectionIO[Entity] =
    upsertMany(NonEmptyList.one(entity))
      .map(_.headOption)
      .flatMap {
        case Some(entity) => connection.pure(entity)
        case None         => connection.raiseError(new IllegalStateException("Entity could not be fetched after its upsert"))
      }
}
