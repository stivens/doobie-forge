package io.github.stivens.forge.interface

import cats.data.NonEmptyList
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait CanCreate[Entity] extends CanCreate.Generic[Entity] with ConnectionIOEffect

object CanCreate {
  trait Generic[Entity] extends Effect {
    def create(entity: Entity): Eff[Entity]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}

trait CanCreateMany[Entity] extends CanCreateMany.Generic[Entity] with ConnectionIOEffect

object CanCreateMany {
  trait Generic[Entity] extends Effect {
    def createMany(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def createMany(entities: List[Entity]): Eff[List[Entity]]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}

trait CanCreateManyWithOnConflictDoHandle[Entity] extends CanCreateManyWithOnConflictDoHandle.Generic[Entity] with ConnectionIOEffect

object CanCreateManyWithOnConflictDoHandle {
  trait Generic[Entity] extends Effect {
    def createManyWithOnConflictDoHandle(entities: NonEmptyList[Entity]): Eff[List[Entity]]
    def createManyWithOnConflictDoHandle(entities: List[Entity]): Eff[List[Entity]]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}

trait CreateOps[Entity]
    extends CreateOps.Generic[Entity]
    with CanCreate[Entity]
    with CanCreateMany[Entity]
    with CanCreateManyWithOnConflictDoHandle[Entity]

object CreateOps {
  trait Generic[Entity]
      extends CanCreate.Generic[Entity]
      with CanCreateMany.Generic[Entity]
      with CanCreateManyWithOnConflictDoHandle.Generic[Entity]

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
