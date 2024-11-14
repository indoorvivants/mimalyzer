package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.boolean
import smithy4s.schema.Schema.struct

final case class Test(id: TestId, attributes: TestAttributes, deleted: Boolean = false)

object Test extends ShapeTag.Companion[Test] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "Test")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(id: TestId, attributes: TestAttributes, deleted: Boolean): Test = Test(id, attributes, deleted)

  implicit val schema: Schema[Test] = struct(
    TestId.schema.required[Test]("id", _.id),
    TestAttributes.schema.required[Test]("attributes", _.attributes),
    boolean.field[Test]("deleted", _.deleted).addHints(smithy.api.Default(smithy4s.Document.fromBoolean(false))),
  )(make).withId(id).addHints(hints)
}
