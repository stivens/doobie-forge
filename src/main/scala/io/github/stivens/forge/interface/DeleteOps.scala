package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait DeleteOps[Entity, ID] extends DeleteOps.Generic[Entity, ID] with ConnectionIOEffect

object DeleteOps {
  trait Generic[Entity, ID] extends Effect {
    def deleteMany(ids: NonEmptyList[ID]): Eff[List[Entity]]
    def deleteMany(ids: List[ID]): Eff[List[Entity]]
    def delete(id: ID): Eff[Option[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}
