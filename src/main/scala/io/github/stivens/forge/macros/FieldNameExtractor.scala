package io.github.stivens.forge.macros

import doobie.syntax.string.*
import doobie.util.Write
import doobie.util.fragment.Fragment
import io.github.stivens.forge.util.DoobieUtil

import scala.quoted.*

/**
 * Utility for extracting field names from case class field accessors.
 * 
 * Usage examples:
 * {{{
 * case class MyClass(foo: String, bar: String, buzz: Int)
 * 
 * // Using type-safe selector
 * FieldNameExtractor.fieldName[MyClass](_.bar) // returns "bar"
 * }}}
 */
object FieldNameExtractor {
  transparent inline def fieldName[T](inline selector: T => Any): String =
    ${ fieldNameImpl[T]('selector) }

  transparent inline def frFieldName[T](inline selector: T => Any): Fragment =
    DoobieUtil.safeConst0Quoted(fieldName[T](selector).toLowerCase)

  transparent inline def frFieldNameOp[T, V: Write](inline selector: T => V, op: Fragment, value: V): Fragment =
    fr"${frFieldName[T](selector)} $op $value"

  transparent inline def frFieldNameOp[T, V: Write](value: V, op: Fragment, inline selector: T => V): Fragment =
    fr"$value $op ${frFieldName[T](selector)}"

  def fieldNameImpl[T: Type](selector: Expr[T => Any])(using Quotes): Expr[String] = {
    import quotes.reflect.*

    def extractFieldName(term: Term): Option[String] = term match {
      case Select(_, name)      => Some(name)
      case Inlined(_, _, block) => extractFieldName(block)
      case Block(ls, _) =>
        ls match {
          case (defdef: DefDef) :: _ =>
            defdef match {
              case DefDef(_, _, _, Some(body)) => extractFieldName(body)
              case _                           => None
            }
          case _ => None
        }
      case _ => None
    }

    val fieldAsTerm = selector.asTerm
    val fieldName = extractFieldName(fieldAsTerm) match {
      case Some(name) => name
      case None       => report.errorAndAbort(s"Illegal expression: ${fieldAsTerm.show}, expected a field selector, e.g. `_.foo`")
    }

    Expr(fieldName)
  }
}
