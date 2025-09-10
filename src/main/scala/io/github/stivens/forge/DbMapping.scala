package io.github.stivens.forge

/**
 * Trait defining bidirectional mapping between domain entities and database entities.
 * 
 * This trait provides the contract for converting between domain entities (business models)
 * and database entities (database table representations). It's used by AbstractRepository
 * to handle the conversion between the two representations during database operations.
 * 
 * The mapping is bidirectional, allowing conversion in both directions:
 * - `dbToEntity`: Converts database entities to domain entities (for reads)
 * - `entityToDb`: Converts domain entities to database entities (for writes)
 * 
 * @tparam Entity The domain entity type representing the business model
 * @tparam DbEntity The database entity type representing the database table structure
 * 
 * @example
 * {{{
 * case class User(id: Long, name: String, email: EmailAddress)
 * case class DbUser(id: Long, user_name: String, email_address: String)
 * 
 * object UserMapping extends DbMapping[User, DbUser] {
 *   def dbToEntity(db: DbUser): User = 
 *     User(id = db.id, name = db.user_name, email = EmailAddress.fromString(db.email_address))
 *   
 *   def entityToDb(entity: User): DbUser = 
 *     DbUser(id = entity.id, user_name = entity.name, email_address = entity.email.toString)
 * }
 * }}}
 */
trait DbMapping[Entity, DbEntity] {

  /**
   * Converts a database entity to a domain entity.
   * 
   * This method is typically used when reading data from the database,
   * converting the database representation to the business model representation.
   * 
   * @param db The database entity to convert
   * @return The corresponding domain entity
   */
  def dbToEntity(db: DbEntity): Entity

  /**
   * Converts a domain entity to a database entity.
   * 
   * This method is typically used when writing data to the database,
   * converting the business model representation to the database representation.
   * 
   * @param entity The domain entity to convert
   * @return The corresponding database entity
   */
  def entityToDb(entity: Entity): DbEntity
}
