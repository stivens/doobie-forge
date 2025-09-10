package io.github.stivens.forge

case class EntityNotFoundError[Entity, ID](id: ID) extends Throwable {
  override def getMessage: String = s"Entity $id not found"
}
