package io.github.stivens.forge.interface

import doobie.ConnectionIO

trait FilterOps[Entity, FilterType] {
  def getManyByFilter(filter: FilterType): ConnectionIO[List[Entity]]
  def countByFilter(filter: FilterType): ConnectionIO[Int]
}
