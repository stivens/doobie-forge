package io.github.stivens.forge.interface

import doobie.ConnectionIO

trait UpsertOps[Entity] {
  def upsert(entity: Entity): ConnectionIO[Entity]
}
