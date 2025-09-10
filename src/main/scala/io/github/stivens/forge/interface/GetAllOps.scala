package io.github.stivens.forge.interface

import doobie.ConnectionIO

trait GetAllOps[Entity] {
  def getAll: ConnectionIO[List[Entity]]
  def countAll: ConnectionIO[Int]
}
