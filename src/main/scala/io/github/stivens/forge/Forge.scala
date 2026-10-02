package io.github.stivens.forge

import doobie.*

import scala.reflect.ClassTag

/** Forge's base classes, mixins and interfaces bound to one effect type, e.g. `object RepositoryTemplate extends Forge with ZioEffect`. */
trait Forge extends EffectBinding { forge =>

  private[forge] trait BoundEffect extends EffectBinding {
    final type Eff[A] = forge.Eff[A]
    final protected def transact[A](io: ConnectionIO[A]): Eff[A] = forge.transact(io)
  }

  abstract class Simple[Entity <: Product](
      tableName: String
  )(using
      Read[Entity],
      Write[Entity],
      ClassTag[Entity]
  ) extends AbstractRepository.Generic[Entity](tableName = tableName)
      with BoundEffect

  abstract class WithIntermediateType[Entity, DbEntity <: Product](
      tableName: String,
      dbMapping: DbMapping[Entity, DbEntity]
  )(using
      Read[DbEntity],
      Write[DbEntity],
      ClassTag[DbEntity],
      ClassTag[Entity]
  ) extends AbstractRepository.Generic.WithIntermediateType[Entity, DbEntity](tableName = tableName, dbMapping = dbMapping)
      with BoundEffect

  object View {
    abstract class Simple[Entity <: Product](
        tableName: String
    )(using
        Read[Entity],
        ClassTag[Entity]
    ) extends AbstractView.Generic[Entity](tableName = tableName)
        with BoundEffect

    abstract class WithIntermediateType[Entity, DbEntity <: Product](
        tableName: String,
        dbToEntity: DbEntity => Entity
    )(using
        Read[DbEntity],
        ClassTag[DbEntity],
        ClassTag[Entity]
    ) extends AbstractView.Generic.WithIntermediateType[Entity, DbEntity](tableName = tableName, dbToEntity = dbToEntity)
        with BoundEffect
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
}
