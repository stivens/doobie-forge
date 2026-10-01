package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.Effectful

trait CreateOps[Entity] extends Effectful {
  def createMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
  def createMany(entities: List[Entity]): Eff[List[Entity]]
  def create(entity: Entity): Eff[Entity]
  def createManyWithOnConflictDoHandle(entities: NonEmptyList[Entity]): Eff[List[Entity]]
  def createManyWithOnConflictDoHandle(entities: List[Entity]): Eff[List[Entity]]
}

object CreateOps {
  type Of[F[_], Entity] = CreateOps[Entity] { type Eff[A] = F[A] }
}
