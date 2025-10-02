package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.constant

final case class Completed()

object Completed extends ShapeTag.Companion[Completed] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "Completed")

  val hints: Hints = Hints.empty


  implicit val schema: Schema[Completed] = constant(Completed()).withId(id).addHints(hints)
}
