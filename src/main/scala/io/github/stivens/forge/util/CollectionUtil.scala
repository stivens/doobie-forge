package io.github.stivens.forge.util

import cats.data.NonEmptyList
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.free.connection
import io.github.stivens.forge.EntityNotFoundError

object CollectionUtil {
  extension [A](wrapped: List[A]) {
    private[forge] def mapNelOrEmpty[B](f: NonEmptyList[A] => ConnectionIO[List[B]]): ConnectionIO[List[B]] =
      wrapped.toNel.fold(connection.pure(List.empty[B]))(f)

    def toMapBy[B](uniquePropertySelector: A => B): Map[B, A] =
      wrapped.groupBy(uniquePropertySelector).collect {
        case (id, List(item)) => (id, item)
        case (id, items) =>
          throw new IllegalArgumentException(s"Could not hash by given property, multiple items landed in the same bucket: $id -> $items")
      }
  }

  extension [A](wrapped: ConnectionIO[List[A]]) {
    private[forge] def headOrFail(what: String): ConnectionIO[A] =
      wrapped.flatMap(_.headOption.liftTo[ConnectionIO](new IllegalStateException(s"Entity could not be fetched after its $what")))
  }

  extension [A](wrapped: ConnectionIO[Option[A]]) {
    private[forge] def orNotFound[ID](id: ID): ConnectionIO[A] =
      wrapped.flatMap(_.liftTo[ConnectionIO](EntityNotFoundError[A, ID](id)))
  }
}
