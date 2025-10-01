package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetStatusInput(id: ComparisonId)

object GetStatusInput extends ShapeTag.Companion[GetStatusInput] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "GetStatusInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(id: ComparisonId): GetStatusInput = GetStatusInput(id)

  implicit val schema: Schema[GetStatusInput] = struct(
    ComparisonId.schema.required[GetStatusInput]("id", _.id).addHints(smithy.api.HttpLabel()),
  )(make).withId(id).addHints(hints)
}
