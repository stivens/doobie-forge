package io.github.stivens.forge.interface

import doobie.implicits.toSqlInterpolator
import doobie.util.fragment.Fragment
import io.github.stivens.forge.ConnectionIOEffect
import io.github.stivens.forge.Effect

trait FSPOps[Entity, FilterType, Order, Cursor] extends FSPOps.Generic[Entity, FilterType, Order, Cursor] with ConnectionIOEffect

object FSPOps {
  trait Generic[Entity, FilterType, Order, Cursor] extends Effect {
    final type FSPRequestType  = FSPRequest[FilterType, Order, Cursor]
    final type FSPResponseType = FSPResponse[Entity, Cursor]

    def fsp(request: FSPRequestType): Eff[FSPResponseType]
  }

  type Of[F[_], Entity, FilterType, Order, Cursor] = Generic[Entity, FilterType, Order, Cursor] { type Eff[A] = F[A] }

  case class FSPResponse[Entity, Cursor](
      entities: List[Entity],
      hasNextPage: Boolean,
      nextPageCursor: Option[Cursor]
  )

  case class FSPRequest[+FilterType, +Order, +Cursor](
      pageSize: Option[Int] = None,
      after: Option[Cursor] = None,
      filter: Option[FilterType] = None,
      sort: Option[SortDefinition[Order]] = None
  )

  case class SortDefinition[+Order](
      by: Order,
      ascOrDesc: AscOrDesc = AscOrDesc.ASC
  ) {
    val frAscOrDesc: Fragment = ascOrDesc match {
      case AscOrDesc.ASC  => fr"ASC"
      case AscOrDesc.DESC => fr"DESC"
    }
    val frSortSign: Fragment = ascOrDesc match {
      case AscOrDesc.ASC  => fr">"
      case AscOrDesc.DESC => fr"<"
    }
  }

  enum AscOrDesc {
    case ASC, DESC
  }
}
