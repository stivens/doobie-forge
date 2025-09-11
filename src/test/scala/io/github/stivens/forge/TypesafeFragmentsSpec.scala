package io.github.stivens.forge

import doobie.*
import doobie.implicits.*
import org.scalatest.funspec.AnyFunSpec

class TypesafeFragmentsSpec extends AnyFunSpec {
  describe("given TypesafeFragments trait") {
    case class Order(
        orderId: Long,
        clientId: Long,
        value: Double,
        description: String
    )
    object Order extends TypesafeFragments[Order]

    case class Client(
        clientId: Long,
        name: String,
        sex: String
    )
    object Client extends TypesafeFragments[Client]

    it("should interpolate field names (explicit type selector)") {
      assert {
        fr"SELECT ${Order.f(_.orderId)}, ${Order.f(_.clientId)} FROM orders".toString ==
          """Fragment("SELECT "orderid", "clientid" FROM orders ")"""
      }
    }

    it("should interpolate field names (imported from case class)") {
      import Order.*
      assert {
        fr"SELECT ${f(_.orderId)}, ${f(_.clientId)} FROM orders".toString ==
          """Fragment("SELECT "orderid", "clientid" FROM orders ")"""
      }
    }

    it("should interpolate field names with alias (explicit type selector)") {
      assert {
        fr"""
            SELECT
              ${Order.fieldWithAlias("order")(_.orderId)},
              ${Order.fieldWithAlias("order")(_.clientId)},
              ${Client.fieldWithAlias("client")(_.name)}
              FROM orders AS "order"
              JOIN clients AS client
              ON ${Order.fieldWithAlias("order")(_.clientId)} = ${Client.fieldWithAlias("client")(_.clientId)}
        """.toString.replaceAll("\\s+", " ") ==
          """Fragment(" SELECT "order"."orderid" , "order"."clientid" , "client"."name" FROM orders AS "order" JOIN clients AS client ON "order"."clientid" = "client"."clientid" ")"""
      }
    }

    it("should interpolate field names with alias (with defined inline alias)") {
      inline def order(inline selector: Order => Any): Fragment   = Order.fieldWithAlias("order")(selector)
      inline def client(inline selector: Client => Any): Fragment = Client.fieldWithAlias("client")(selector)

      assert {
        fr"""
            SELECT
            ${order(_.orderId)},
            ${order(_.clientId)},
            ${client(_.name)}
            FROM orders AS "order"
            JOIN clients AS client
            ON ${order(_.clientId)} = ${client(_.clientId)}
        """.toString.replaceAll("\\s+", " ") ==
          """Fragment(" SELECT "order"."orderid" , "order"."clientid" , "client"."name" FROM orders AS "order" JOIN clients AS client ON "order"."clientid" = "client"."clientid" ")"""
      }
    }

    it("should interpolate frSet/frEq fragments") {
      import Order.*
      assert {
        fr"UPDATE orders SET ${frSet(_.value, 99.99)} WHERE ${frEq(_.orderId, 1L)}".toString.replaceAll("\\s+", " ") ==
          """Fragment("UPDATE orders SET "value" = ? WHERE "orderid" = ? ")"""
      }
    }

    it("should interpolate frOp fragments") {
      import Order.*
      assert {
        fr"SELECT * FROM orders WHERE ${frOp(_.value, fr">", 99.99)}".toString.replaceAll("\\s+", " ") ==
          """Fragment("SELECT * FROM orders WHERE "value" > ? ")"""
      }
      assert {
        fr"SELECT * FROM orders WHERE ${frOp(99.99, fr">", _.value)}".toString.replaceAll("\\s+", " ") ==
          """Fragment("SELECT * FROM orders WHERE ? > "value" ")"""
      }
    }
  }
}
