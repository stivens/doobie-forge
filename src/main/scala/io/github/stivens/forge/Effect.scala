package io.github.stivens.forge

import doobie.ConnectionIO

trait Effect {
  type Eff[A]
}

trait ConnectionIOEffect extends Effect {
  final type Eff[A] = ConnectionIO[A]
}

trait EffectBinding extends Effect {
  protected def transact[A](io: ConnectionIO[A]): Eff[A]
}

trait ConnectionIOBinding extends EffectBinding with ConnectionIOEffect {
  final protected def transact[A](io: ConnectionIO[A]): ConnectionIO[A] = io
}
