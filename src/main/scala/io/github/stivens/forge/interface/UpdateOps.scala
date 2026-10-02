package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty

trait UpdateOps[Entity, ID, UpdateType] extends UpdateOps.Generic[Entity, ID, UpdateType] with ConnectionIOEffect {
  def updateMany(ids: List[ID], update: UpdateType): ConnectionIO[List[Entity]] =
    ids.mapNelOrEmpty(updateMany(_, update))

  def update(id: ID, update: UpdateType): ConnectionIO[Option[Entity]] =
    updateMany(NonEmptyList.one(id), update).map(_.headOption)
}

object UpdateOps {
  trait Generic[Entity, ID, UpdateType] extends Effect {
    def updateMany(ids: NonEmptyList[ID], update: UpdateType): Eff[List[Entity]]
    def updateMany(ids: List[ID], update: UpdateType): Eff[List[Entity]]
    def update(id: ID, update: UpdateType): Eff[Option[Entity]]
  }

  type Of[F[_], Entity, ID, UpdateType] = Generic[Entity, ID, UpdateType] { type Eff[A] = F[A] }
}
