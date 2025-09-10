package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import doobie.free.connection
import io.github.stivens.forge.EntityNotFoundError
import io.github.stivens.forge.util.CollectionUtil.mapNelOrSucceedWith

trait GetByIdOps[Entity, ID] {
  def getById(id: ID): ConnectionIO[Option[Entity]]

  final def getByIdOrFail(id: ID): ConnectionIO[Entity] = for {
    maybeEntity <- getById(id)
    entity <- maybeEntity match {
      case None         => connection.raiseError(EntityNotFoundError[Entity, ID](id))
      case Some(entity) => connection.pure(entity)
    }
  } yield entity

  def getManyByIds(ids: NonEmptyList[ID]): ConnectionIO[List[Entity]]

  final def getManyByIds(ids: List[ID]): ConnectionIO[List[Entity]] =
    ids.mapNelOrSucceedWith(idsNel => getManyByIds(idsNel), default = List.empty)
}
