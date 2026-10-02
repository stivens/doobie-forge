package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import doobie.ConnectionIO
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty

trait DeleteOps[Entity, ID] extends DeleteOps.Generic[Entity, ID] with ConnectionIOEffect {
  final def deleteMany(ids: List[ID]): ConnectionIO[List[Entity]] =
    ids.mapNelOrEmpty(deleteMany(_))

  final def delete(id: ID): ConnectionIO[Option[Entity]] =
    deleteMany(NonEmptyList.one(id)).map(_.headOption)
}

object DeleteOps {
  trait Generic[Entity, ID] extends Effect {
    def deleteMany(ids: NonEmptyList[ID]): Eff[List[Entity]]
    def deleteMany(ids: List[ID]): Eff[List[Entity]]
    def delete(id: ID): Eff[Option[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}
