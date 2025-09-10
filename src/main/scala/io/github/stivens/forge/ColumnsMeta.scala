package io.github.stivens.forge

import cats.syntax.all.*
import doobie.Fragment
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.util.DoobieUtil

import scala.reflect.ClassTag

/**
 * Trait that provides metadata about database table columns derived from a case class.
 * 
 * This trait uses reflection to automatically extract column names from a case class's
 * primary constructor parameters. It's designed to work with case classes that represent
 * database entities, automatically generating SQL fragments for column references.
 * 
 * The trait filters out synthetic parameters (those starting with "$") and allows
 * exclusion of specific columns by name. It provides multiple representations of the
 * column information for different use cases in SQL generation.
 * 
 * @tparam A The case class type representing the database entity
 * @param excluded Set of column names to exclude from the metadata (defaults to empty set)
 * @param classTag Implicit ClassTag for the case class type
 * 
 * @example
 * {{{
 * case class User(id: Long, name: String, email: String, createdAt: OffsetDateTime)
 * 
 * object UserColumns extends ColumnsMeta[User](excluded = Set("createdAt"))
 * 
 * // UserColumns.columnsList = List(fr"id", fr"name", fr"email")
 * // UserColumns.frColumns = fr"id, name, email"
 * }}}
 */
trait ColumnsMeta[A](excluded: Set[String] = Set.empty)(using classTag: ClassTag[A]) {

  /**
   * List of column names extracted from the case class constructor parameters.
   * 
   * This property uses reflection to:
   * 1. Get the primary constructor of the case class
   * 2. Extract parameter names and convert them to lowercase
   * 3. Filter out synthetic parameters (starting with "$")
   * 4. Exclude any columns specified in the `excluded` set
   * 
   * @return A list of column names as strings
   */
  val columnsList: List[String] = {
    val primaryConstructor = classTag.runtimeClass.getDeclaredConstructors.head
    val constructorParams  = primaryConstructor.getParameters.toList.map(_.getName.toLowerCase)
    constructorParams.collect {
      case column if !column.startsWith("$") && !excluded.contains(column) => column
    }
  }

  /**
   * List of column names converted to Doobie Fragment objects.
   * 
   * Each column name is wrapped in a Fragment using `DoobieUtil.safeConst0Quoted`
   * to ensure proper SQL escaping and quoting. This is useful when you need
   * individual column fragments for operations like INSERT statements.
   * 
   * @return A list of Fragment objects representing individual columns
   */
  val columnsListAsFragments: List[Fragment] = columnsList.map(DoobieUtil.safeConst0Quoted)

  /**
   * SQL fragment representing all columns as a comma-separated list.
   * 
   * This is the most commonly used property, providing a Fragment that can be
   * directly used in SELECT statements, INSERT statements, and other SQL operations
   * that require a column list.
   * 
   * @return A Fragment representing "column1, column2, column3, ..."
   */
  val frColumns: Fragment = columnsListAsFragments.intercalate(fr", ")
}
