package mimalyzer.protocol

import java.util.UUID
import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.uuid

object ComparisonId extends Newtype[UUID] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "ComparisonId")
  val hints: Hints = Hints(
    alloy.UuidFormat(),
  ).lazily
  val underlyingSchema: Schema[UUID] = uuid.withId(id).addHints(hints)
  implicit val schema: Schema[ComparisonId] = bijection(underlyingSchema, asBijection)
}
