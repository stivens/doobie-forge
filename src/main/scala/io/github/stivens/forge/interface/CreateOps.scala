package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait CreateOps[Entity] extends CreateOps.Generic[Entity] with ConnectionIOEffect

object CreateOps {
  trait Generic[Entity] extends Effect {
    def createMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def createMany(entities: List[Entity]): Eff[List[Entity]]
    def create(entity: Entity): Eff[Entity]
    def createManyWithOnConflictDoHandle(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def createManyWithOnConflictDoHandle(entities: List[Entity]): Eff[List[Entity]]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
