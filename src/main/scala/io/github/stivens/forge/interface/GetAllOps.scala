package io.github.stivens.forge.interface

import io.github.stivens.forge.ConnectionIOEffectful
import io.github.stivens.forge.Effectful

trait GetAllOps[Entity] extends GetAllOps.Generic[Entity] with ConnectionIOEffectful

object GetAllOps {
  trait Generic[Entity] extends Effectful {
    def getAll: Eff[List[Entity]]
    def countAll: Eff[Int]
  }

  type Of[F[_], Entity] = Generic[Entity] { type Eff[A] = F[A] }
}
