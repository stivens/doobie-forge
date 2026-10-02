package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait CanUpsert[Entity] extends CanUpsert.Generic[Entity] with ConnectionIOEffect

object CanUpsert {
  trait Generic[Entity] extends Effect {
    def upsert(entity: Entity): Eff[Entity]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}

trait CanUpsertMany[Entity] extends CanUpsertMany.Generic[Entity] with ConnectionIOEffect

object CanUpsertMany {
  trait Generic[Entity] extends Effect {
    def upsertMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def upsertMany(entities: List[Entity]): Eff[List[Entity]]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}

trait UpsertOps[Entity] extends UpsertOps.Generic[Entity] with CanUpsert[Entity] with CanUpsertMany[Entity]

object UpsertOps {
  trait Generic[Entity] extends CanUpsert.Generic[Entity] with CanUpsertMany.Generic[Entity]

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
