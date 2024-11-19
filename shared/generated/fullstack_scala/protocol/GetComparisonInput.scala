package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetComparisonInput(id: ComparisonId)

object GetComparisonInput extends ShapeTag.Companion[GetComparisonInput] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "GetComparisonInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(id: ComparisonId): GetComparisonInput = GetComparisonInput(id)

  implicit val schema: Schema[GetComparisonInput] = struct(
    ComparisonId.schema.required[GetComparisonInput]("id", _.id).addHints(smithy.api.HttpLabel()),
  )(make).withId(id).addHints(hints)
}
