package io.github.stivens.forge.mixins

import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.Fragments.*
import doobie.implicits.*
import io.github.stivens.forge.*
import io.github.stivens.forge.testsetup.transactor
import io.github.stivens.forge.util.DoobieUtil.safeConst0Quoted
import org.scalatest.funspec.AnyFunSpec

class JoinedSpec extends AnyFunSpec {
  describe("given view with Joined mixin") {
    case class Order(
        orderId: Long,
        clientId: Long,
        value: Double,
        description: String
    ) derives Read,
          Write

    case class OrderFilter(
        value_gte: Option[Double] = None,
        value_lte: Option[Double] = None,
        clientId_eq: Option[Long] = None,
        clientName_like: Option[String] = None,
        clientSex_eq: Option["F" | "M"] = None
    )

    object OrderView
        extends AbstractView.Simple[Order](fr"orders")
        with IdentifiedBy[Order, Long](_.orderId)
        with Joined(
          frAlias = safeConst0Quoted("order"),
          frJoin = fr"""JOIN clients AS client ON "order".clientId = client.clientId"""
        )
        with Filtering[Order, OrderFilter](handleFilter =
          toFragments[OrderFilter]
            .usingNonEmpty(_.value_gte)(value => fr"value >= ${value}")
            .usingNonEmpty(_.value_lte)(value => fr"value <= ${value}")
            .usingNonEmpty(_.clientId_eq)(clientId => fr"clientId = ${clientId}")
            .usingNonEmpty(_.clientName_like)(clientName => fr"client.name LIKE ${%%(clientName)}")
            .usingNonEmpty(_.clientSex_eq)(clientSex => fr"client.sex = ${clientSex}")
            .compile
        )
    // init relation
    sql"""
        DROP TABLE IF EXISTS orders;
        CREATE TABLE orders (
            orderId SERIAL PRIMARY KEY,
            clientId BIGINT NOT NULL,
            value DECIMAL(10, 2) NOT NULL,
            description VARCHAR(255) NOT NULL
        );
        DROP TABLE IF EXISTS clients;
        CREATE TABLE clients (
            clientId SERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            sex VARCHAR(1) NOT NULL
        );

        INSERT INTO clients (clientId, name, sex) VALUES
        (1, 'John Doe', 'M'),
        (2, 'Jane Doe', 'F'),
        (3, 'Jim Beam', 'M'),
        (4, 'Jill Bean', 'F');

        INSERT INTO orders (orderId, clientId, value, description) VALUES
        (1, 1, 100.0, 'Order 1'),
        (2, 2, 200.0, 'Order 2'),
        (3, 3, 300.0, 'Order 3'),
        (4, 4, 400.0, 'Order 4'),
        (5, 1, 500.0, 'Order 5');
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    it("should filter & get data") {
      val filterBySex = OrderFilter(clientSex_eq = Some("M"))
      val resultBySex = OrderView.getManyByFilter(filterBySex).transact(transactor).unsafeRunSync()

      val filterByClientName = OrderFilter(clientName_like = Some("Ji"))
      val resultByClientName = OrderView.getManyByFilter(filterByClientName).transact(transactor).unsafeRunSync()

      assert(resultBySex.map(_.orderId).sorted == List(1, 3, 5))
      assert(resultByClientName.map(_.orderId).sorted == List(3, 4))
    }
  }
}
