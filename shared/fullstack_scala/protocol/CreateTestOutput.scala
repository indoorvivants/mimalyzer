package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CreateTestOutput(test: Test)

object CreateTestOutput extends ShapeTag.Companion[CreateTestOutput] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "CreateTestOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(test: Test): CreateTestOutput = CreateTestOutput(test)

  implicit val schema: Schema[CreateTestOutput] = struct(
    Test.schema.required[CreateTestOutput]("test", _.test),
  )(make).withId(id).addHints(hints)
}
