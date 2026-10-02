package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait CanGetById[Entity, ID] extends CanGetById.Generic[Entity, ID] with ConnectionIOEffect

object CanGetById {
  trait Generic[Entity, ID] extends Effect {
    def getById(id: ID): Eff[Option[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}

trait CanGetByIdOrFail[Entity, ID] extends CanGetByIdOrFail.Generic[Entity, ID] with ConnectionIOEffect

object CanGetByIdOrFail {
  trait Generic[Entity, ID] extends Effect {
    def getByIdOrFail(id: ID): Eff[Entity]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}

trait CanGetManyByIds[Entity, ID] extends CanGetManyByIds.Generic[Entity, ID] with ConnectionIOEffect

object CanGetManyByIds {
  trait Generic[Entity, ID] extends Effect {
    def getManyByIds(ids: NonEmptyList[ID]): Eff[List[Entity]]
    def getManyByIds(ids: List[ID]): Eff[List[Entity]]
  }

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}

trait GetByIdOps[Entity, ID]
    extends GetByIdOps.Generic[Entity, ID]
    with CanGetById[Entity, ID]
    with CanGetByIdOrFail[Entity, ID]
    with CanGetManyByIds[Entity, ID]

object GetByIdOps {
  trait Generic[Entity, ID]
      extends CanGetById.Generic[Entity, ID]
      with CanGetByIdOrFail.Generic[Entity, ID]
      with CanGetManyByIds.Generic[Entity, ID]

  type Of[F[_], Entity, ID] = Generic[Entity, ID] { type Eff[A] = F[A] }
}
