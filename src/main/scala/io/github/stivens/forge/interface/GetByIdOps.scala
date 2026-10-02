package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait GetByIdOps[Entity, ID] extends GetByIdOps.Generic[Entity, ID] with ConnectionIOEffect

object GetByIdOps {
  trait Generic[Entity, ID] extends Effect {
    def getById(id: ID): Eff[Option[Entity]]
    def getByIdOrFail(id: ID): Eff[Entity]
    def getManyByIds(ids: NonEmptyList[ID]): Eff[List[Entity]]
    def getManyByIds(ids: List[ID]): Eff[List[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}
