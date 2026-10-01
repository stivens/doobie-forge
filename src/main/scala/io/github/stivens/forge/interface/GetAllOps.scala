package io.github.stivens.forge.interface

import io.github.stivens.forge.Effectful

trait GetAllOps[Entity] extends Effectful {
  def getAll: Eff[List[Entity]]
  def countAll: Eff[Int]
}

object GetAllOps {
  type Of[F[_], Entity] = GetAllOps[Entity] { type Eff[A] = F[A] }
}
