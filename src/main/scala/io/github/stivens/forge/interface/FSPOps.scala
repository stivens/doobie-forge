package io.github.stivens.forge.interface

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.fragment.Fragment
import io.github.stivens.forge.interface.FSPOps.FSPRequest
import io.github.stivens.forge.interface.FSPOps.FSPResponse

trait FSPOps[Entity, FilterType, Order, Cursor] {
  final type FSPRequestType  = FSPRequest[FilterType, Order, Cursor]
  final type FSPResponseType = FSPResponse[Entity, Cursor]

  def fsp(request: FSPRequestType): ConnectionIO[FSPResponseType]
  def filteredCount(request: FSPRequestType): ConnectionIO[Int]
}

object FSPOps {
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
