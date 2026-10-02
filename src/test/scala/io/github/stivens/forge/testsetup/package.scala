package io.github.stivens.forge

import cats.*
import cats.effect.*
import doobie.*
import doobie.implicits.*
import zio.Task
import zio.interop.catz.*

package object testsetup {
  trait IOEffect extends EffectBinding {
    final type Eff[A] = IO[A]
    final protected def transact[A](io: ConnectionIO[A]): IO[A] = io.transact(transactor)
  }

  trait ZIOEffect extends EffectBinding {
    final type Eff[A] = Task[A]
    final protected def transact[A](io: ConnectionIO[A]): Task[A] = io.transact(zioTransactor)
  }

  val transactor    = driverManagerTransactor[IO]
  val zioTransactor = driverManagerTransactor[Task]

  private def driverManagerTransactor[F[_]: Async] = Transactor.fromDriverManager[F](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5431/test?sslmode=disable",
    user = "postgres",
    password = "postgres",
    logHandler = None
  )

  def unsafeRunZIO[A](task: Task[A]): A =
    zio.Unsafe.unsafe(implicit unsafe => zio.Runtime.default.unsafe.run(task).getOrThrowFiberFailure())
}
