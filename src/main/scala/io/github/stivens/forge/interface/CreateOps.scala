package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import io.github.stivens.forge.ConnectionIOEffectful
import io.github.stivens.forge.Effectful
import io.github.stivens.forge.util.CollectionUtil.headOrFail
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty

trait CreateOps[Entity] extends CreateOps.Generic[Entity] with ConnectionIOEffectful {
  final def createMany(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrEmpty(createMany(_))

  final def create(entity: Entity): ConnectionIO[Entity] =
    createMany(NonEmptyList.one(entity)).headOrFail("creation")

  final def createManyWithOnConflictDoHandle(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrEmpty(createManyWithOnConflictDoHandle(_))
}

object CreateOps {
  trait Generic[Entity] extends Effectful {
    def createMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def createMany(entities: List[Entity]): Eff[List[Entity]]
    def create(entity: Entity): Eff[Entity]
    def createManyWithOnConflictDoHandle(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def createManyWithOnConflictDoHandle(entities: List[Entity]): Eff[List[Entity]]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
