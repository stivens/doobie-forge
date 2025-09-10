package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import doobie.free.connection
import io.github.stivens.forge.util.CollectionUtil.mapNelOrSucceedWith

trait CreateOps[Entity] {
  def createMany(entities: NonEmptyList[Entity]): ConnectionIO[List[Entity]]

  final def createMany(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrSucceedWith(entitiesNel => createMany(entitiesNel), default = List.empty)

  final def create(entity: Entity): ConnectionIO[Entity] = for {
    result <- createMany(NonEmptyList.one(entity))
    createdEntity <- result.headOption match {
      case None         => connection.raiseError(new IllegalStateException("Entity could not be fetched after its creation"))
      case Some(entity) => connection.pure(entity)
    }
  } yield createdEntity

  def createManyWithOnConflictDoHandle(entities: NonEmptyList[Entity]): ConnectionIO[List[Entity]]

  final def createManyWithOnConflictDoHandle(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrSucceedWith(entitiesNel => createManyWithOnConflictDoHandle(entitiesNel), default = List.empty)
}
