package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class HealthOutput(status: String)

object HealthOutput extends ShapeTag.Companion[HealthOutput] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "HealthOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(status: String): HealthOutput = HealthOutput(status)

  implicit val schema: Schema[HealthOutput] = struct(
    string.required[HealthOutput]("status", _.status),
  )(make).withId(id).addHints(hints)
}
