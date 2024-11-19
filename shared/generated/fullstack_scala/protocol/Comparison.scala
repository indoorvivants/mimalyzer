package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class Comparison(id: ComparisonId, attributes: ComparisonAttributes)

object Comparison extends ShapeTag.Companion[Comparison] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "Comparison")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(id: ComparisonId, attributes: ComparisonAttributes): Comparison = Comparison(id, attributes)

  implicit val schema: Schema[Comparison] = struct(
    ComparisonId.schema.required[Comparison]("id", _.id),
    ComparisonAttributes.schema.required[Comparison]("attributes", _.attributes),
  )(make).withId(id).addHints(hints)
}
