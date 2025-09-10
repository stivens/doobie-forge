package io.github.stivens.forge.util

import cats.data.NonEmptyList
import cats.syntax.list.catsSyntaxList
import doobie.ConnectionIO
import doobie.free.connection

object CollectionUtil {
  extension [A](wrapped: List[A]) {
    def mapNelOrSucceedWith[B](f: NonEmptyList[A] => ConnectionIO[B], default: B): ConnectionIO[B] =
      catsSyntaxList(wrapped).toNel.map(f).getOrElse(connection.pure(default))
  }
}
