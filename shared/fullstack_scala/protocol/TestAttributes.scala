package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class TestAttributes(title: TestTitle, description: Option[TestDescription] = None)

object TestAttributes extends ShapeTag.Companion[TestAttributes] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "TestAttributes")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(title: TestTitle, description: Option[TestDescription]): TestAttributes = TestAttributes(title, description)

  implicit val schema: Schema[TestAttributes] = struct(
    TestTitle.schema.required[TestAttributes]("title", _.title),
    TestDescription.schema.optional[TestAttributes]("description", _.description),
  )(make).withId(id).addHints(hints)
}
