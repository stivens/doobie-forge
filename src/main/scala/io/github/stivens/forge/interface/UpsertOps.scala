package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import io.github.stivens.forge.ConnectionIOEffectful
import io.github.stivens.forge.Effectful
import io.github.stivens.forge.util.CollectionUtil.headOrFail
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty

trait UpsertOps[Entity] extends UpsertOps.Generic[Entity] with ConnectionIOEffectful {
  final def upsertMany(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrEmpty(upsertMany(_))

  final def upsert(entity: Entity): ConnectionIO[Entity] =
    upsertMany(NonEmptyList.one(entity)).headOrFail("upsert")
}

object UpsertOps {
  trait Generic[Entity] extends Effectful {
    def upsertMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def upsertMany(entities: List[Entity]): Eff[List[Entity]]
    def upsert(entity: Entity): Eff[Entity]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
