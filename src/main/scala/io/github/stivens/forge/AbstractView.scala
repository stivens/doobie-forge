package io.github.stivens.forge

import doobie.*
import doobie.implicits.toSqlInterpolator
import fs2.Stream
import io.github.stivens.forge.interface.*
import io.github.stivens.forge.util.DoobieUtil.*

import scala.reflect.ClassTag

/**
 * Abstract base class for database view operations that provides read-only access to database entities.
 * 
 * This class serves as the foundation for implementing database views and read-only repositories.
 * It provides common functionality for querying database tables and converting database entities
 * to domain entities.
 * 
 * @tparam Entity The domain entity type that represents the business model
 * @tparam DbEntity The database entity type that represents the database table structure (must be a Product)
 * 
 * @param tableName The database table name as a Doobie Fragment
 * @param dbToEntity Function to convert database entities to domain entities
 */
abstract class AbstractView[Entity, DbEntity <: Product](
    final protected val tableName: String,
    final protected val dbToEntity: DbEntity => Entity
)(using
    read: Read[DbEntity],
    dbEntityClassTag: ClassTag[DbEntity]
) extends GetAllOps.Generic[Entity]
    with EffectBinding {

  final override def getAll: Eff[List[Entity]] = transact(getAllC)
  final override def countAll: Eff[Int]        = transact(countAllC)

  final protected def getAllC: ConnectionIO[List[Entity]] =
    selectWith(fragment = Fragment.empty)

  final protected def countAllC: ConnectionIO[Int] =
    getCountWhere(whereClause = Fragment.empty)

  protected object columnsMeta extends ColumnsMeta[DbEntity]()
  import columnsMeta.*

  protected val frTableName: Fragment              = safeConst(tableName)
  protected val fr0TableName: Fragment             = safeConst0(tableName)
  protected def frSelectColumnsFromTable: Fragment = fr"SELECT $frColumns FROM $frTableName"
  protected def frSelectCountFromTable: Fragment   = fr"SELECT COUNT(*) FROM $frTableName"

  final protected def selectWith(fragment: Fragment): ConnectionIO[List[Entity]] =
    runSelect(frSelectColumnsFromTable ++ fragment)

  final protected def getCountWhere(whereClause: Fragment): ConnectionIO[Int] =
    (frSelectCountFromTable ++ whereClause).query[Int].unique

  final protected def runSelect(sql: Fragment): ConnectionIO[List[Entity]] =
    sql
      .query[DbEntity]
      .map(dbToEntity)
      .to[List]

  final protected def runSelectStream(sql: Fragment): Stream[ConnectionIO, Entity] =
    sql
      .query[DbEntity]
      .stream
      .map(dbToEntity)

  final protected val _readDbEntity: Read[DbEntity] = read
}

object AbstractView {
  abstract class WithIntermediateType[Entity, DbEntity <: Product](
      tableName: String,
      dbToEntity: DbEntity => Entity
  )(using
      Read[DbEntity],
      ClassTag[DbEntity]
  ) extends AbstractView[Entity, DbEntity](tableName = tableName, dbToEntity = dbToEntity)
      with ConnectionIOBinding
      with GetAllOps[Entity]

  /**
   * Simplified version of AbstractView for cases where the domain entity and database entity are the same.
   * 
   * This is useful when you don't need separate domain and database representations,
   * and the entity can be used directly as both.
   * 
   * @tparam Entity The entity type (must be a Product)
   * @param tableName The database table name as a Doobie Fragment
   */
  abstract class Simple[Entity <: Product](
      tableName: String
  )(using
      Read[Entity],
      ClassTag[Entity]
  ) extends WithIntermediateType[Entity, Entity](tableName = tableName, dbToEntity = identity)

  /** Like [[Simple]], but leaves `Eff` abstract. Prefer building repositories from a [[Forge]] over extending this directly. */
  abstract class Generic[Entity <: Product](
      tableName: String
  )(using
      Read[Entity],
      ClassTag[Entity]
  ) extends AbstractView[Entity, Entity](tableName = tableName, dbToEntity = identity)
}
