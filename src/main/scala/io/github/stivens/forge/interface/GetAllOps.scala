package io.github.stivens.forge.interface

import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait CanGetAll[Entity] extends CanGetAll.Generic[Entity] with ConnectionIOEffect

object CanGetAll {
  trait Generic[Entity] extends Effect {
    def getAll: Eff[List[Entity]]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}

trait CanCountAll[Entity] extends CanCountAll.Generic[Entity] with ConnectionIOEffect

object CanCountAll {
  trait Generic[Entity] extends Effect {
    def countAll: Eff[Int]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}

trait GetAllOps[Entity] extends GetAllOps.Generic[Entity] with CanGetAll[Entity] with CanCountAll[Entity]

object GetAllOps {
  trait Generic[Entity] extends CanGetAll.Generic[Entity] with CanCountAll.Generic[Entity]

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
