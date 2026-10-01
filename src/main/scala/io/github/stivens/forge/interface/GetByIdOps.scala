package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.Effectful

trait GetByIdOps[Entity, ID] extends Effectful {
  def getById(id: ID): Eff[Option[Entity]]
  def getByIdOrFail(id: ID): Eff[Entity]
  def getManyByIds(ids: NonEmptyList[ID]): Eff[List[Entity]]
  def getManyByIds(ids: List[ID]): Eff[List[Entity]]
}

object GetByIdOps {
  type Of[F[_], Entity, ID] = GetByIdOps[Entity, ID] { type Eff[A] = F[A] }
}
