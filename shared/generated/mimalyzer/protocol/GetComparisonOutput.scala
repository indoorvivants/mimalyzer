package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetComparisonOutput(comparison: Comparison)

object GetComparisonOutput extends ShapeTag.Companion[GetComparisonOutput] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "GetComparisonOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(comparison: Comparison): GetComparisonOutput = GetComparisonOutput(comparison)

  implicit val schema: Schema[GetComparisonOutput] = struct(
    Comparison.schema.required[GetComparisonOutput]("comparison", _.comparison),
  )(make).withId(id).addHints(hints)
}
