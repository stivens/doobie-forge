package io.github.stivens.forge

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import doobie.*
import doobie.implicits.*
import io.github.stivens.forge.mixins.*
import io.github.stivens.forge.testsetup.transactor
import org.scalatest.funspec.AnyFunSpec

class ComplexPrimaryKeySpec extends AnyFunSpec {
  describe("given repository with a complex primary key") {
    type ResourceId = Long
    type UserId     = Long

    case class ResourceAssignment(
        resourceId: ResourceId,
        userId: UserId,
        description: Option[String]
    ) derives Read,
          Write

    object ResourceAssignmentRepository
        extends AbstractRepository.Simple[ResourceAssignment](tableName = "resource_assignments")
        with IdentifiedBy[ResourceAssignment, (ResourceId, UserId)](
          extractId = a => (a.resourceId, a.userId),
          frId = fr"(resourceId, userId)"
        )
        with Deletions[ResourceAssignment, (ResourceId, UserId)]
        with Upsertions[ResourceAssignment]

    // init relation
    sql"""
        DROP TABLE IF EXISTS resource_assignments;
        CREATE TABLE resource_assignments (
            resourceId BIGINT NOT NULL,
            userId BIGINT NOT NULL,
            description TEXT NULL,
            PRIMARY KEY (resourceId, userId)
        );
    """.update.run.map(_ => ()).transact(transactor).unsafeRunSync()

    val resourceAssignments = List(
      ResourceAssignment(resourceId = 1, userId = 1, description = Some("Assignment 1")),
      ResourceAssignment(resourceId = 2, userId = 2, description = Some("Assignment 2")),
      ResourceAssignment(resourceId = 3, userId = 3, description = Some("Assignment 3")),
      ResourceAssignment(resourceId = 4, userId = 4, description = Some("Assignment 4"))
    )

    it("should create and get entities") {
      val createResult  = ResourceAssignmentRepository.createMany(resourceAssignments).transact(transactor).unsafeRunSync()
      val getAllResult  = ResourceAssignmentRepository.getAll.transact(transactor).unsafeRunSync()
      val getByIdResult = ResourceAssignmentRepository.getById((1, 1)).transact(transactor).unsafeRunSync()

      assert(createResult == resourceAssignments)
      assert(getAllResult == resourceAssignments)
      assert(getByIdResult == Some(resourceAssignments(0)))
    }

    it("should upsert an entity") {
      val updatedResourceAssignment = resourceAssignments(0).copy(description = Some("Assignment 1 updated"))
      val upsertResult              = ResourceAssignmentRepository.upsert(updatedResourceAssignment).transact(transactor).unsafeRunSync()
      val getByIdResult             = ResourceAssignmentRepository.getById((1, 1)).transact(transactor).unsafeRunSync()

      assert(upsertResult == updatedResourceAssignment)
      assert(getByIdResult == Some(updatedResourceAssignment))
    }

    it("should delete by id") {
      val deleteResult = ResourceAssignmentRepository.delete((1, 1)).transact(transactor).unsafeRunSync()
      val getAllResult = ResourceAssignmentRepository.getAll.transact(transactor).unsafeRunSync()

      assert(deleteResult.isDefined)
      assert(getAllResult == resourceAssignments.tail)
    }

  }
}
