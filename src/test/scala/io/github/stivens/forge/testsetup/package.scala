package io.github.stivens.forge

import cats.*
import cats.effect.*
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.EffectLift

package object testsetup {
  trait IOEffect extends EffectLift {
    type Eff[A] = IO[A]
    final protected def lift[A](io: ConnectionIO[A]): IO[A] = io.transact(transactor)
  }

  val transactor = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5431/test?sslmode=disable",
    user = "postgres",
    password = "postgres",
    logHandler = None
  )
}
