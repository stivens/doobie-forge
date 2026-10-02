package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty
import io.github.stivens.forge.util.CollectionUtil.orNotFound

trait GetByIdOps[Entity, ID] extends GetByIdOps.Generic[Entity, ID] with ConnectionIOEffect {
  final def getByIdOrFail(id: ID): ConnectionIO[Entity] =
    getById(id).orNotFound(id)

  final def getManyByIds(ids: List[ID]): ConnectionIO[List[Entity]] =
    ids.mapNelOrEmpty(getManyByIds(_))
}

object GetByIdOps {
  trait Generic[Entity, ID] extends Effect {
    def getById(id: ID): Eff[Option[Entity]]
    def getByIdOrFail(id: ID): Eff[Entity]
    def getManyByIds(ids: NonEmptyList[ID]): Eff[List[Entity]]
    def getManyByIds(ids: List[ID]): Eff[List[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}
