package io.github.stivens.forge.interface

import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait FilterOps[Entity, FilterType] extends FilterOps.Generic[Entity, FilterType] with ConnectionIOEffect

object FilterOps {
  trait Generic[Entity, FilterType] extends Effect {
    def getManyByFilter(filter: FilterType): Eff[List[Entity]]
    def countByFilter(filter: FilterType): Eff[Int]
  }

  type Of[F[_], Entity, FilterType] = Generic[Entity, FilterType] { type Eff[A] = F[A] }
}
