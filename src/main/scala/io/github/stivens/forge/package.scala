package io.github.stivens

import doobie.Fragment
import io.github.stivens.casecomplete.CaseComplete
import io.github.stivens.casecomplete.macros.CaseCompleteBuilder

package object forge {
  type AsFragments[A <: Product] = CaseComplete[A, Option[Fragment]]
  def toFragments[A <: Product]: CaseCompleteBuilder[A, Option[Fragment], EmptyTuple] = CaseComplete.build[A, Option[Fragment]]

  def %%(str: String): String = s"%$str%"
}
