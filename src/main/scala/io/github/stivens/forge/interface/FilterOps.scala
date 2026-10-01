package io.github.stivens.forge.interface

import io.github.stivens.forge.ConnectionIOEffectful
import io.github.stivens.forge.Effectful

trait FilterOps[Entity, FilterType] extends FilterOps.Generic[Entity, FilterType] with ConnectionIOEffectful

object FilterOps {
  trait Generic[Entity, FilterType] extends Effectful {
    def getManyByFilter(filter: FilterType): Eff[List[Entity]]
    def countByFilter(filter: FilterType): Eff[Int]
  }

  type Of[F[_], Entity, FilterType] = Generic[Entity, FilterType] { type Eff[A] = F[A] }
}
