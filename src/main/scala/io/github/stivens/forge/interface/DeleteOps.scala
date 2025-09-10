package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import io.github.stivens.forge.util.CollectionUtil.mapNelOrSucceedWith

trait DeleteOps[Entity, ID] {
  def deleteMany(ids: NonEmptyList[ID]): ConnectionIO[List[Entity]]

  final def deleteMany(ids: List[ID]): ConnectionIO[List[Entity]] =
    ids.mapNelOrSucceedWith(idsNel => deleteMany(idsNel), default = List.empty)

  final def delete(id: ID): ConnectionIO[Option[Entity]] =
    deleteMany(NonEmptyList.one(id)).map(_.headOption)
}
