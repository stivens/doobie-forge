package io.github.stivens.forge

import doobie.ConnectionIO

trait Effectful {
  type Eff[A]
}

trait ConnectionIOEffectful extends Effectful {
  final type Eff[A] = ConnectionIO[A]
}

trait EffectLift extends Effectful {
  protected def lift[A](io: ConnectionIO[A]): Eff[A]
}

trait ConnectionIOEffect extends EffectLift with ConnectionIOEffectful {
  final protected def lift[A](io: ConnectionIO[A]): ConnectionIO[A] = io
}
