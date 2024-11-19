package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CreateComparisonInput(attributes: ComparisonAttributes)

object CreateComparisonInput extends ShapeTag.Companion[CreateComparisonInput] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "CreateComparisonInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(attributes: ComparisonAttributes): CreateComparisonInput = CreateComparisonInput(attributes)

  implicit val schema: Schema[CreateComparisonInput] = struct(
    ComparisonAttributes.schema.required[CreateComparisonInput]("attributes", _.attributes),
  )(make).withId(id).addHints(hints)
}
