package io.github.stivens.forge.mixins

import cats.data.NonEmptyList
import cats.syntax.all.*
import doobie.*
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.AbstractView

/** A mixin trait that provides JOIN capabilities for database views.
  *
  * This trait enables views to perform queries with JOIN operations, automatically handling
  * table aliases and ensuring proper DISTINCT behavior when joining tables. It modifies the
  * base query structure to include JOIN clauses and table aliases.
  *
  * @param frAlias A SQL fragment representing the table alias for the main table
  * @param frJoin A SQL fragment representing the JOIN clause (e.g., "JOIN other_table AS other ON ...")
  * @param withSortedByJoinedColumns Optional additional columns to include in SELECT for ORDER BY compatibility
  *                                   (required when using SELECT DISTINCT with ORDER BY on joined columns)
  *
  * @example {{{
  *   case class Order(orderId: Long, clientId: Long, value: Double, description: String) derives Read, Write
  *   case class OrderFilter(
  *     value_gte: Option[Double] = None,
  *     clientName_like: Option[String] = None,
  *     clientSex_eq: Option["F" | "M"] = None
  *   )
  *
  *   object OrderView extends AbstractView.Simple[Order](fr"orders")
  *     with IdentifiedBy[Order, Long](_.orderId)
  *     with Joined(
  *       frAlias = safeConst0Quoted("order"),
  *       frJoin = fr"""JOIN clients AS client ON "order".clientId = client.clientId"""
  *     )
  *     with Filtering[Order, OrderFilter](handleFilter = toFragments[OrderFilter]
  *       .usingNonEmpty(_.value_gte)(value => fr"value >= ${value}")
  *       .usingNonEmpty(_.clientName_like)(clientName => fr"client.name LIKE ${%%(clientName)}")
  *       .usingNonEmpty(_.clientSex_eq)(clientSex => fr"client.sex = ${clientSex}")
  *       .compile)
  *
  *   // Now you can filter by joined table columns:
  *   val filter = OrderFilter(clientSex_eq = Some("M"), clientName_like = Some("John"))
  *   val orders: List[Order] = OrderView.getManyByFilter(filter).transact(transactor).unsafeRunSync()
  * }}}
  *
  * @note Uses SELECT DISTINCT to handle potential duplicates from JOIN operations
  * @note When using ORDER BY on joined columns, those columns must be included in the SELECT list
  */
trait Joined(
    frAlias: Fragment,
    frJoin: Fragment,
    withSortedByJoinedColumns: Option[Fragment] = None // for SELECT DISTINCT, ORDER BY expressions must appear in select list
) {
  this: AbstractView[?, ?] & IdentifiedBy.Core[?, ?] =>

  final protected val frTableNameWithAlias = fr"$frTableName AS $frAlias"
  final protected val frIdWithAlias        = fr"$frAlias.$frId"

  protected lazy val columsWithAliasList: List[Fragment] = columnsMeta.columnsListAsFragments.map(c => fr"$frAlias.$c")
  protected lazy val frColumnsWithAlias: Fragment        = columsWithAliasList.intercalate(fr", ")

  /** Overrides the base SELECT query to include JOIN operations and DISTINCT behavior.
    *
    * This method modifies the base query structure to:
    * - Use SELECT DISTINCT to handle potential duplicates from JOINs
    * - Include table aliases for proper column referencing
    * - Add JOIN clauses for related table access
    * - Optionally include additional columns for ORDER BY compatibility
    */
  override protected def frSelectColumnsFromTable: Fragment = withSortedByJoinedColumns match {
    case None =>
      fr"SELECT DISTINCT $frColumnsWithAlias FROM $frTableNameWithAlias $frJoin"
    case Some(additionalColumns) =>
      fr"SELECT DISTINCT $frColumnsWithAlias, $additionalColumns FROM $frTableNameWithAlias $frJoin"
  }

  /** Overrides the base COUNT query to include JOIN operations and DISTINCT behavior.
    *
    * This method modifies the count query to:
    * - Use COUNT(DISTINCT id) to avoid counting duplicates from JOINs
    * - Include table aliases for proper column referencing
    * - Add JOIN clauses for related table access
    */
  override protected def frSelectCountFromTable: Fragment = fr"SELECT COUNT(DISTINCT $frIdWithAlias) FROM $frTableNameWithAlias $frJoin"

  /** Overrides the ID-based filtering to use aliased ID column.
    *
    * This ensures that ID-based operations (like getManyByIds) work correctly
    * with the joined table structure by using the aliased ID column.
    */
  override protected def frIdsIn(ids: NonEmptyList[_ID]): Fragment = in(frIdWithAlias, ids)
}
