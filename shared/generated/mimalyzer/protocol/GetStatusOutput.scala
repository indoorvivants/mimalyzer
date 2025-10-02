package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class GetStatusOutput(status: ComparisonStatus)

object GetStatusOutput extends ShapeTag.Companion[GetStatusOutput] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "GetStatusOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(status: ComparisonStatus): GetStatusOutput = GetStatusOutput(status)

  implicit val schema: Schema[GetStatusOutput] = struct(
    ComparisonStatus.schema.required[GetStatusOutput]("status", _.status),
  )(make).withId(id).addHints(hints)
}
