package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.Effectful

trait UpdateOps[Entity, ID, UpdateType] extends Effectful {
  def updateMany(ids: NonEmptyList[ID], update: UpdateType): Eff[List[Entity]]
  def updateMany(ids: List[ID], update: UpdateType): Eff[List[Entity]]
  def update(id: ID, update: UpdateType): Eff[Option[Entity]]
}

object UpdateOps {
  type Of[F[_], Entity, ID, UpdateType] = UpdateOps[Entity, ID, UpdateType] { type Eff[A] = F[A] }
}
