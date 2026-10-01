package io.github.stivens.forge.mixins

import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.AbstractRepository
import io.github.stivens.forge.testsetup.transactor
import io.github.stivens.forge.util.DoobieUtil.safeConst0Quoted
import org.scalatest.funspec.AnyFunSpec

class DeletionsSpec extends AnyFunSpec {
  describe("given repository with Deletions mixin") {
    case class User(id: Long, name: String) derives Read, Write

    object UserRepository
        extends AbstractRepository.Simple[User](tableName = "users_to_be_deleted")
        with IdentifiedBy[User, Long](_.id)
        with Deletions[User, Long]

    // init relation
    sql"""
        DROP TABLE IF EXISTS users_to_be_deleted;
        CREATE TABLE users_to_be_deleted (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL);
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val users = List(
      User(id = 1, name = "John"),
      User(id = 2, name = "Jane"),
      User(id = 3, name = "Jim"),
      User(id = 4, name = "Jill")
    )

    UserRepository.createMany(users).transact(transactor).unsafeRunSync()

    it("should delete by id") {
      val deleteResult = UserRepository.delete(1).transact(transactor).unsafeRunSync()
      val getAllResult = UserRepository.getAll.transact(transactor).unsafeRunSync()

      assert(deleteResult == Some(User(id = 1, name = "John")))
      assert(getAllResult == users.tail)
    }

    it("should delete many by ids") {
      val deleteResult = UserRepository.deleteMany(List(2L, 3L)).transact(transactor).unsafeRunSync()
      val getAllResult = UserRepository.getAll.transact(transactor).unsafeRunSync()

      assert(deleteResult == List(User(id = 2, name = "Jane"), User(id = 3, name = "Jim")))
      assert(getAllResult == List(User(id = 4, name = "Jill")))
    }
  }

  describe("given joined repository with Deletions mixin") {
    case class Order(orderId: Long, description: String) derives Read, Write

    object OrderRepository
        extends AbstractRepository.Simple[Order](tableName = "joined_orders_to_be_deleted")
        with IdentifiedBy[Order, Long](_.orderId, fr"orderId")
        with Joined(frAlias = safeConst0Quoted("order"), frJoin = Fragment.empty)
        with Deletions[Order, Long]

    // init relation
    sql"""
        DROP TABLE IF EXISTS joined_orders_to_be_deleted;
        CREATE TABLE joined_orders_to_be_deleted (orderId SERIAL PRIMARY KEY, description VARCHAR(255) NOT NULL);
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val orders = List(
      Order(orderId = 1, description = "Order 1"),
      Order(orderId = 2, description = "Order 2"),
      Order(orderId = 3, description = "Order 3")
    )

    OrderRepository.createMany(orders).transact(transactor).unsafeRunSync()

    it("should delete many by ids") {
      val deleteResult = OrderRepository.deleteMany(List(1L, 2L)).transact(transactor).unsafeRunSync()
      val getAllResult = OrderRepository.getAll.transact(transactor).unsafeRunSync()

      assert(deleteResult == orders.take(2))
      assert(getAllResult == orders.drop(2))
    }
  }
}
