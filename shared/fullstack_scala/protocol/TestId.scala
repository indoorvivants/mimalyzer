package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.int

object TestId extends Newtype[Int] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "TestId")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[Int] = int.withId(id).addHints(hints)
  implicit val schema: Schema[TestId] = bijection(underlyingSchema, asBijection)
}
