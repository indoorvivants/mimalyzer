package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.string

object ScalaCode extends Newtype[String] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "ScalaCode")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[String] = string.withId(id).addHints(hints)
  implicit val schema: Schema[ScalaCode] = bijection(underlyingSchema, asBijection)
}
