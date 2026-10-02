package io.github.stivens.forge.interface

import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait CanGetManyByFilter[Entity, FilterType] extends CanGetManyByFilter.Generic[Entity, FilterType] with ConnectionIOEffect

object CanGetManyByFilter {
  trait Generic[Entity, FilterType] extends Effect {
    def getManyByFilter(filter: FilterType): Eff[List[Entity]]
  }

  type Of[F[_], Entity, FilterType] = Generic[Entity, FilterType] { type Eff[A] = F[A] }
}

trait CanCountByFilter[Entity, FilterType] extends CanCountByFilter.Generic[Entity, FilterType] with ConnectionIOEffect

object CanCountByFilter {
  trait Generic[Entity, FilterType] extends Effect {
    def countByFilter(filter: FilterType): Eff[Int]
  }

  type Of[F[_], Entity, FilterType] = Generic[Entity, FilterType] { type Eff[A] = F[A] }
}

trait FilterOps[Entity, FilterType]
    extends FilterOps.Generic[Entity, FilterType]
    with CanGetManyByFilter[Entity, FilterType]
    with CanCountByFilter[Entity, FilterType]

object FilterOps {
  trait Generic[Entity, FilterType] extends CanGetManyByFilter.Generic[Entity, FilterType] with CanCountByFilter.Generic[Entity, FilterType]

  type Of[F[_], Entity, FilterType] = Generic[Entity, FilterType] { type Eff[A] = F[A] }
}
