package io.github.stivens

import cats.data.NonEmptyList
import cats.syntax.all.*
import doobie.Fragment
import io.github.stivens.casecomplete.CaseComplete
import io.github.stivens.casecomplete.macros.CaseCompleteBuilder

package object forge {
  type AsFragments[A <: Product] = CaseComplete[A, Option[Fragment]]
  def toFragments[A <: Product]: CaseCompleteBuilder[A, Option[Fragment], EmptyTuple] = CaseComplete.build[A, Option[Fragment]]

  def %%(str: String): String = s"%$str%"

  final val DEFAUL_FSP_PAGE_SIZE = 100

  extension [A <: Product, B](builderOfOptional: CaseCompleteBuilder[A, Option[B], ?]) {
    transparent inline def usingNonEmptyList[F](
        inline field: A => Option[List[F]]
    )(handler: NonEmptyList[F] => B): CaseCompleteBuilder[A, Option[B], ?] =
      builderOfOptional.using[Option[List[F]]](field)(opt => opt.flatMap(ls => ls.toNel.map(handler)))
  }
}
