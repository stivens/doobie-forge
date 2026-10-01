package io.github.stivens.forge.interface

import io.github.stivens.forge.Effectful

trait FilterOps[Entity, FilterType] extends Effectful {
  def getManyByFilter(filter: FilterType): Eff[List[Entity]]
  def countByFilter(filter: FilterType): Eff[Int]
}

object FilterOps {
  type Of[F[_], Entity, FilterType] = FilterOps[Entity, FilterType] { type Eff[A] = F[A] }
}
