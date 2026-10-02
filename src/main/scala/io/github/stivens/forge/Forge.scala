package io.github.stivens.forge

import doobie.*

import scala.reflect.ClassTag

/** Forge's base classes, mixins and interfaces bound to one effect type, e.g. `object RepositoryTemplate extends Forge with ZioEffect`. */
trait Forge extends EffectBinding { self =>

  trait Bound extends EffectBinding {
    final type Eff[A] = self.Eff[A]
    final protected def transact[A](io: ConnectionIO[A]): Eff[A] = self.transact(io)
  }

  abstract class Simple[Entity <: Product](
      tableName: String
  )(using
      Read[Entity],
      Write[Entity],
      ClassTag[Entity]
  ) extends WithIntermediateType[Entity, Entity](tableName = tableName, dbMapping = AbstractRepository.identityMapping)

  abstract class WithIntermediateType[Entity, DbEntity <: Product](
      tableName: String,
      dbMapping: DbMapping[Entity, DbEntity]
  )(using
      Read[DbEntity],
      Write[DbEntity],
      ClassTag[DbEntity]
  ) extends AbstractRepository[Entity, DbEntity](tableName = tableName, dbMapping = dbMapping)
      with Bound

  object View {
    abstract class Simple[Entity <: Product](
        tableName: String
    )(using
        Read[Entity],
        ClassTag[Entity]
    ) extends WithIntermediateType[Entity, Entity](tableName = tableName, dbToEntity = identity)

    abstract class WithIntermediateType[Entity, DbEntity <: Product](
        tableName: String,
        dbToEntity: DbEntity => Entity
    )(using
        Read[DbEntity],
        ClassTag[DbEntity]
    ) extends AbstractView[Entity, DbEntity](tableName = tableName, dbToEntity = dbToEntity)
        with Bound
  }

  type IdentifiedBy[Entity, ID]                 = mixins.IdentifiedBy.Generic[Entity, ID]
  type Filtering[Entity, FilterType <: Product] = mixins.Filtering.Generic[Entity, FilterType]
  type FSP[Entity, DbEntity <: Product, FilterType <: Product, Order, Cursor] =
    mixins.FSP.Generic[Entity, DbEntity, FilterType, Order, Cursor]
  type Updates[Entity, ID, UpdateType <: Product] = mixins.Updates.Generic[Entity, ID, UpdateType]
  type Deletions[Entity, ID]                      = mixins.Deletions.Generic[Entity, ID]
  type Upsertions[Entity]                         = mixins.Upsertions.Generic[Entity]
  type Joined                                     = mixins.Joined

  type GetAllOps[Entity]                         = interface.GetAllOps.Of[Eff, Entity]
  type GetByIdOps[Entity, ID]                    = interface.GetByIdOps.Of[Eff, Entity, ID]
  type CreateOps[Entity]                         = interface.CreateOps.Of[Eff, Entity]
  type FilterOps[Entity, FilterType]             = interface.FilterOps.Of[Eff, Entity, FilterType]
  type FSPOps[Entity, FilterType, Order, Cursor] = interface.FSPOps.Of[Eff, Entity, FilterType, Order, Cursor]
  type UpdateOps[Entity, ID, UpdateType]         = interface.UpdateOps.Of[Eff, Entity, ID, UpdateType]
  type DeleteOps[Entity, ID]                     = interface.DeleteOps.Of[Eff, Entity, ID]
  type UpsertOps[Entity]                         = interface.UpsertOps.Of[Eff, Entity]

  type CanGetAll[Entity]                           = interface.CanGetAll.Of[Eff, Entity]
  type CanCountAll[Entity]                         = interface.CanCountAll.Of[Eff, Entity]
  type CanGetById[Entity, ID]                      = interface.CanGetById.Of[Eff, Entity, ID]
  type CanGetByIdOrFail[Entity, ID]                = interface.CanGetByIdOrFail.Of[Eff, Entity, ID]
  type CanGetManyByIds[Entity, ID]                 = interface.CanGetManyByIds.Of[Eff, Entity, ID]
  type CanCreate[Entity]                           = interface.CanCreate.Of[Eff, Entity]
  type CanCreateMany[Entity]                       = interface.CanCreateMany.Of[Eff, Entity]
  type CanCreateManyWithOnConflictDoHandle[Entity] = interface.CanCreateManyWithOnConflictDoHandle.Of[Eff, Entity]
  type CanGetManyByFilter[Entity, FilterType]      = interface.CanGetManyByFilter.Of[Eff, Entity, FilterType]
  type CanCountByFilter[Entity, FilterType]        = interface.CanCountByFilter.Of[Eff, Entity, FilterType]
  type CanUpdate[Entity, ID, UpdateType]           = interface.CanUpdate.Of[Eff, Entity, ID, UpdateType]
  type CanUpdateMany[Entity, ID, UpdateType]       = interface.CanUpdateMany.Of[Eff, Entity, ID, UpdateType]
  type CanDelete[Entity, ID]                       = interface.CanDelete.Of[Eff, Entity, ID]
  type CanDeleteMany[Entity, ID]                   = interface.CanDeleteMany.Of[Eff, Entity, ID]
  type CanUpsert[Entity]                           = interface.CanUpsert.Of[Eff, Entity]
  type CanUpsertMany[Entity]                       = interface.CanUpsertMany.Of[Eff, Entity]
}
