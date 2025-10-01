package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Waiting()

object Waiting extends ShapeTag.Companion[Waiting] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "Waiting")

  val hints: Hints = Hints.empty


  implicit val schema: Schema[Waiting] = constant(Waiting()).withId(id).addHints(hints)
}
