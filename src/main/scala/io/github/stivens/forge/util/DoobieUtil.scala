package io.github.stivens.forge.util

import doobie.Fragment
import doobie.util.fragment.Fragment.const
import doobie.util.fragment.Fragment.const0

object DoobieUtil {
  private val allowedNames = raw"([a-zA-Z0-9_{}\"]*)".r

  /**
    * Throws RuntimeError unless name is only composed of allowed characters.
    */
  private def validate: String => String = {
    case allowedNames(name) => name
    case other =>
      throw new RuntimeException(
        s"Implementation error: name `$other` contains characters that can't / shouldn't be processed as SQL column name"
      )
  }

  def safeConst(input: String): Fragment        = const(validate(input))
  def safeConst0(input: String): Fragment       = const0(validate(input))
  def safeConst0Quoted(input: String): Fragment = safeConst0(s"\"$input\"")
  def safeNumberConst0(input: Int): Fragment    = const0(input.toString)

}
