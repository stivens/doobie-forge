package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import io.github.stivens.forge.util.CollectionUtil.mapNelOrSucceedWith

trait UpdateOps[Entity, ID, UpdateType] {
  def updateMany(ids: NonEmptyList[ID], update: UpdateType): ConnectionIO[List[Entity]]

  def updateMany(ids: List[ID], update: UpdateType): ConnectionIO[List[Entity]] =
    ids.mapNelOrSucceedWith(idsNel => updateMany(idsNel, update), default = List.empty)

  final def update(id: ID, update: UpdateType): ConnectionIO[Option[Entity]] =
    updateMany(NonEmptyList.one(id), update)
      .map(_.headOption)
}
