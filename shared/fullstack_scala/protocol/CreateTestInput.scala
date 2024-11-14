package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CreateTestInput(attributes: TestAttributes)

object CreateTestInput extends ShapeTag.Companion[CreateTestInput] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "CreateTestInput")

  val hints: Hints = Hints(
    smithy.api.Input(),
  ).lazily

  // constructor using the original order from the spec
  private def make(attributes: TestAttributes): CreateTestInput = CreateTestInput(attributes)

  implicit val schema: Schema[CreateTestInput] = struct(
    TestAttributes.schema.required[CreateTestInput]("attributes", _.attributes),
  )(make).withId(id).addHints(hints)
}
