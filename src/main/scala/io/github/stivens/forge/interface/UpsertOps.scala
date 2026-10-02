package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait UpsertOps[Entity] extends UpsertOps.Generic[Entity] with ConnectionIOEffect

object UpsertOps {
  trait Generic[Entity] extends Effect {
    def upsertMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def upsertMany(entities: List[Entity]): Eff[List[Entity]]
    def upsert(entity: Entity): Eff[Entity]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
