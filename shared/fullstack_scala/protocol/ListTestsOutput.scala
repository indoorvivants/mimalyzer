package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ListTestsOutput(tests: List[Test])

object ListTestsOutput extends ShapeTag.Companion[ListTestsOutput] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "ListTestsOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(tests: List[Test]): ListTestsOutput = ListTestsOutput(tests)

  implicit val schema: Schema[ListTestsOutput] = struct(
    Tests.underlyingSchema.required[ListTestsOutput]("tests", _.tests),
  )(make).withId(id).addHints(hints)
}
