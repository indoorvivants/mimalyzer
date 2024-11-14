package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object Tests extends Newtype[List[Test]] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "Tests")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Test]] = list(Test.schema).withId(id).addHints(hints)
  implicit val schema: Schema[Tests] = bijection(underlyingSchema, asBijection)
}
