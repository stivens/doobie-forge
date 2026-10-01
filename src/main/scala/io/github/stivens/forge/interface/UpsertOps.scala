package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.Effectful

trait UpsertOps[Entity] extends Effectful {
  def upsertMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
  def upsertMany(entities: List[Entity]): Eff[List[Entity]]
  def upsert(entity: Entity): Eff[Entity]
}

object UpsertOps {
  type Of[F[_], Entity] = UpsertOps[Entity] { type Eff[A] = F[A] }
}
