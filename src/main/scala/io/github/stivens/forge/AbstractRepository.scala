package io.github.stivens.forge

import cats.data.NonEmptyList
import doobie.*
import doobie.Fragments.*
import doobie.implicits.toSqlInterpolator
import io.github.stivens.forge.interface.*
import io.github.stivens.forge.util.CollectionUtil.headOrFail
import io.github.stivens.forge.util.CollectionUtil.mapNelOrEmpty

import scala.reflect.ClassTag

/**
 * Abstract base class for database repository operations that provides both read and write access to database entities.
 * 
 * This class extends AbstractView to provide read operations and implements the NonEmptyList operations of CreateOps.Generic; extend Simple or WithIntermediateType for ConnectionIO, or Generic for a custom effect.
 * It serves as the foundation for implementing full-featured repositories that can both query and create entities
 * in the database.
 * 
 * The repository handles the conversion between domain entities and database entities using a DbMapping,
 * allowing for clean separation between business models and database representations.
 * 
 * @tparam Entity The domain entity type that represents the business model
 * @tparam DbEntity The database entity type that represents the database table structure (must be a Product)
 * 
 * @param tableName The database table name as a Doobie Fragment
 * @param dbMapping The mapping between domain and database entities
 * 
 * @example
 * {{{
 * case class User(id: Long, name: String, email: String)
 * case class DbUser(id: Long, user_name: String, email_address: String)
 * 
 * 
 * object UserRepository extends AbstractRepository.WithIntermediateType[User, DbUser](
 *   tableName = fr"users",
 *   dbMapping = new DbMapping[User, DbUser] {
 *      def dbToEntity(db: DbUser): User = 
 *        User(id = db.id, name = db.user_name, email = db.email_address)
 *      def entityToDb(entity: User): DbUser = 
 *        DbUser(id = entity.id, user_name = entity.name, email_address = entity.email)
 *    }
 * )
 * }}}
 */
abstract class AbstractRepository[Entity, DbEntity <: Product](
    tableName: String,
    protected val dbMapping: DbMapping[Entity, DbEntity]
)(using
    read: Read[DbEntity],
    write: Write[DbEntity],
    dbEntityClassTag: ClassTag[DbEntity],
    entityClassTag: ClassTag[Entity]
) extends AbstractView[Entity, DbEntity](
      tableName = tableName,
      dbToEntity = dbMapping.dbToEntity
    )
    with CreateOps.Generic[Entity] {

  final override def createMany(entities: NonEmptyList[Entity]): Eff[List[Entity]] = lift(createManyC(entities))

  final override def createManyWithOnConflictDoHandle(entities: NonEmptyList[Entity]): Eff[List[Entity]] =
    lift(createManyWithOnConflictDoHandleC(entities))

  final protected def createManyC(entities: NonEmptyList[Entity]): ConnectionIO[List[Entity]] = {
    val dbEntities = entities.map(dbMapping.entityToDb)
    runUpdateMany(frCreateMany(dbEntities) ++ frWithReturning)
  }

  final protected def createManyC(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrEmpty(createManyC(_))

  final protected def createC(entity: Entity): ConnectionIO[Entity] =
    createManyC(NonEmptyList.one(entity)).headOrFail("creation")

  final protected def createManyWithOnConflictDoHandleC(entities: NonEmptyList[Entity]): ConnectionIO[List[Entity]] = {
    val dbEntities = entities.map(dbMapping.entityToDb)
    runUpdateMany(frCreateMany(dbEntities) ++ frOnConflict ++ frWithReturning)
  }

  final protected def createManyWithOnConflictDoHandleC(entities: List[Entity]): ConnectionIO[List[Entity]] =
    entities.mapNelOrEmpty(createManyWithOnConflictDoHandleC(_))

  import columnsMeta.*

  protected def frWithReturning: Fragment = fr"RETURNING $frColumns"
  protected def frOnConflict: Fragment    = fr"ON CONFLICT DO NOTHING"

  protected def frCreateMany(entities: NonEmptyList[DbEntity]): Fragment =
    sql"""INSERT INTO $frTableName ($frColumns) ${values(entities)}"""

  protected def frCreate(entity: DbEntity): Fragment =
    frCreateMany(NonEmptyList.one(entity))

  final protected def runUpdateMany(sql: Fragment): ConnectionIO[List[Entity]] =
    sql.update
      .withGeneratedKeys[DbEntity](columnsList*)
      .map(dbMapping.dbToEntity)
      .compile
      .toList
}

object AbstractRepository {
  abstract class WithIntermediateType[Entity, DbEntity <: Product](
      tableName: String,
      dbMapping: DbMapping[Entity, DbEntity]
  )(using
      Read[DbEntity],
      Write[DbEntity],
      ClassTag[DbEntity],
      ClassTag[Entity]
  ) extends AbstractRepository[Entity, DbEntity](tableName = tableName, dbMapping = dbMapping)
      with ConnectionIOEffect
      with GetAllOps[Entity]
      with CreateOps[Entity]

  /**
   * Simplified version of AbstractRepository for cases where the domain entity and database entity are the same.
   * 
   * This is useful when you don't need separate domain and database representations,
   * and the entity can be used directly as both. The mapping is implemented as an identity function.
   * 
   * @tparam Entity The entity type (must be a Product)
   * @param tableName The database table name as a Doobie Fragment
   * 
   * @example
   * {{{
   * case class User(id: Long, name: String, email: String)
   * 
   * object UserRepository extends AbstractRepository.Simple[User](
   *   tableName = fr"users"
   * )
   * }}}
   */
  abstract class Simple[Entity <: Product](
      tableName: String
  )(using
      Read[Entity],
      Write[Entity],
      ClassTag[Entity]
  ) extends WithIntermediateType[Entity, Entity](tableName = tableName, dbMapping = identityMapping)

  /** Like [[Simple]], but leaves `Eff` abstract: mix in an [[EffectLift]] implementation to choose the effect type. */
  abstract class Generic[Entity <: Product](
      tableName: String
  )(using
      Read[Entity],
      Write[Entity],
      ClassTag[Entity]
  ) extends Generic.WithIntermediateType[Entity, Entity](tableName = tableName, dbMapping = identityMapping)

  object Generic {
    abstract class WithIntermediateType[Entity, DbEntity <: Product](
        tableName: String,
        dbMapping: DbMapping[Entity, DbEntity]
    )(using
        Read[DbEntity],
        Write[DbEntity],
        ClassTag[DbEntity],
        ClassTag[Entity]
    ) extends AbstractRepository[Entity, DbEntity](tableName = tableName, dbMapping = dbMapping) {
      final override def createMany(entities: List[Entity]): Eff[List[Entity]] = lift(createManyC(entities))
      final override def create(entity: Entity): Eff[Entity]                   = lift(createC(entity))
      final override def createManyWithOnConflictDoHandle(entities: List[Entity]): Eff[List[Entity]] =
        lift(createManyWithOnConflictDoHandleC(entities))
    }
  }

  private def identityMapping[Entity]: DbMapping[Entity, Entity] = new DbMapping[Entity, Entity] {
    def dbToEntity(db: Entity): Entity     = db
    def entityToDb(entity: Entity): Entity = entity
  }
}
