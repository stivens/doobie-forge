package io.github.stivens.forge

import doobie.ConnectionIO

trait Effectful {
  type Eff[A]
}

trait EffectLift extends Effectful {
  protected def lift[A](io: ConnectionIO[A]): Eff[A]
}

trait ConnectionIOEffect extends EffectLift {
  final type Eff[A] = ConnectionIO[A]
  final protected def lift[A](io: ConnectionIO[A]): ConnectionIO[A] = io
}
