package io.github.stivens.forge.interface

import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait GetAllOps[Entity] extends GetAllOps.Generic[Entity] with ConnectionIOEffect

object GetAllOps {
  trait Generic[Entity] extends Effect {
    def getAll: Eff[List[Entity]]
    def countAll: Eff[Int]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
