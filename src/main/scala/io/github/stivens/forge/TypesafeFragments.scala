package io.github.stivens.forge

import doobie.Fragment
import doobie.implicits.toSqlInterpolator
import doobie.util.Write
import io.github.stivens.forge.macros.FieldNameExtractor
import io.github.stivens.forge.util.DoobieUtil.safeConst0Quoted

/** A trait that provides type-safe SQL fragment generation for case classes.
  *
  * This trait enables compile-time extraction of field names from case class selectors,
  * providing type-safe SQL fragment generation without runtime reflection. It uses Scala 3
  * inline macros to extract field names at compile time and generate proper SQL fragments.
  *
  * @tparam A The case class type for which to generate type-safe fragments
  *
  * @example {{{
  *   case class Order(orderId: Long, clientId: Long, value: Double, description: String)
  *   object Order extends TypesafeFragments[Order]
  *
  *   // Basic field name extraction
  *   val orderIdFragment: Fragment = Order.f(_.orderId) // generates: fr"orderid"
  *   val clientIdFragment: Fragment = Order.f(_.clientId) // generates: fr"clientid"
  *
  *   // Field names with table aliases
  *   val orderIdWithAlias: Fragment = Order.fieldWithAlias("order")(_.orderId) // generates: fr""" "order"."orderid" """
  *   val clientIdWithAlias: Fragment = Order.fieldWithAlias("order")(_.clientId) // generates: fr""" "order"."clientid" """
  *
  *   // Equality operations
  *   val eqFragment: Fragment = Order.frEq(_.orderId, 1L) // generates: fr""" "orderid" = 1 """
  *   val setFragment: Fragment = Order.frSet(_.value, 99.99) // generates: fr""" "value" = 99.99 """
  *
  *   // Custom operations
  *   val gtFragment: Fragment = Order.frOp(_.value, fr">", 99.99) // generates: fr""" "value" > 99.99 """
  *   val ltFragment: Fragment = Order.frOp(99.99, fr"<", _.value) // generates: fr""" 99.99 < "value" """
  *
  *   // Usage in SQL queries
  *   inline def order(inline selector: Order => Any): Fragment   = Order.fieldWithAlias("order")(selector)
  *   inline def client(inline selector: Client => Any): Fragment = Client.fieldWithAlias("client")(selector)
  
  *   val query: Fragment = fr"SELECT ${order(_.orderId)}, ${client(_.name)} FROM orders AS "order" JOIN clients AS "client" ON ${order(_.clientId)} = ${client(_.clientId)}"
  * 
  *   val updateQuery: Fragment = fr"UPDATE orders SET ${Order.frSet(_.value, 99.99)} WHERE ${Order.frEq(_.orderId, 1L)}"
  * }}}
  *
  * @note Field names are automatically converted to lowercase and quoted for SQL compatibility
  * @note All operations are compile-time checked - invalid field references will cause compilation errors
  */
trait TypesafeFragments[A] {

  /** Extracts the field name from a case class selector and returns it as a SQL fragment.
    *
    * @param selector A function that selects a field from the case class
    * @return A SQL fragment containing the quoted field name
    */
  inline def f(inline selector: A => Any): Fragment =
    FieldNameExtractor.frFieldName[A](selector)

  /** Extracts the field name with a table alias and returns it as a SQL fragment.
    *
    * @param alias The table alias to prefix the field name
    * @param selector A function that selects a field from the case class
    * @return A SQL fragment containing the aliased field name (e.g., "alias"."fieldname")
    */
  inline def fieldWithAlias(alias: String)(inline selector: A => Any): Fragment =
    fr"${safeConst0Quoted(alias)}.${f(selector)}"

  /** Creates an equality comparison SQL fragment.
    *
    * @param selector A function that selects a field from the case class
    * @param value The value to compare against
    * @return A SQL fragment containing the equality comparison (e.g., "fieldname" = ?)
    */
  inline def frEq[V: Write](inline selector: A => V, value: V): Fragment =
    frOp(selector, fr"=", value)

  /** Creates a SET clause SQL fragment for UPDATE statements.
    *
    * @param selector A function that selects a field from the case class
    * @param value The value to set
    * @return A SQL fragment containing the SET clause (e.g., "fieldname" = ?)
    */
  inline def frSet[V: Write](inline selector: A => V, value: V): Fragment =
    frEq(selector, value)

  /** Creates a custom operation SQL fragment with field name on the left side.
    *
    * @param selector A function that selects a field from the case class
    * @param op The SQL operator fragment (e.g., fr">", fr"<=", fr"LIKE")
    * @param value The value to compare against
    * @return A SQL fragment containing the operation (e.g., "fieldname" > ?)
    */
  inline def frOp[V: Write](inline selector: A => V, op: Fragment, value: V): Fragment =
    FieldNameExtractor.frFieldNameOp[A, V](selector, op, value)

  /** Creates a custom operation SQL fragment with field name on the right side.
    *
    * @param value The value to compare against
    * @param op The SQL operator fragment (e.g., fr">", fr"<=", fr"LIKE")
    * @param selector A function that selects a field from the case class
    * @return A SQL fragment containing the operation (e.g., ? > "fieldname")
    */
  inline def frOp[V: Write](value: V, op: Fragment, inline selector: A => V): Fragment =
    FieldNameExtractor.frFieldNameOp[A, V](value, op, selector)
}
