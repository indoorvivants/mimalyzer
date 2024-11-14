package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object ScalaVersion extends Newtype[String] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "ScalaVersion")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[ScalaVersion] = bijection(underlyingSchema, asBijection)
}
