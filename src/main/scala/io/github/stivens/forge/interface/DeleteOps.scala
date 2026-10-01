package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.Effectful

trait DeleteOps[Entity, ID] extends Effectful {
  def deleteMany(ids: NonEmptyList[ID]): Eff[List[Entity]]
  def deleteMany(ids: List[ID]): Eff[List[Entity]]
  def delete(id: ID): Eff[Option[Entity]]
}

object DeleteOps {
  type Of[F[_], Entity, ID] = DeleteOps[Entity, ID] { type Eff[A] = F[A] }
}
