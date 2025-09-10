package io.github.stivens.forge

import cats.*
import cats.effect.*
import doobie.*

package object testsetup {
  val transactor = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver",
    url = "jdbc:postgresql://localhost:5432/test?sslmode=disable",
    user = "postgres",
    password = "postgres",
    logHandler = None
  )
}
