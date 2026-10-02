package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait CanDelete[Entity, ID] extends CanDelete.Generic[Entity, ID] with ConnectionIOEffect

object CanDelete {
  trait Generic[Entity, ID] extends Effect {
    def delete(id: ID): Eff[Option[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}

trait CanDeleteMany[Entity, ID] extends CanDeleteMany.Generic[Entity, ID] with ConnectionIOEffect

object CanDeleteMany {
  trait Generic[Entity, ID] extends Effect {
    def deleteMany(ids: NonEmptyList[ID]): Eff[List[Entity]]
    def deleteMany(ids: List[ID]): Eff[List[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}

trait DeleteOps[Entity, ID] extends DeleteOps.Generic[Entity, ID] with CanDelete[Entity, ID] with CanDeleteMany[Entity, ID]

object DeleteOps {
  trait Generic[Entity, ID] extends CanDelete.Generic[Entity, ID] with CanDeleteMany.Generic[Entity, ID]

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}
