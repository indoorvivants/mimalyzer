package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class ComparisonAttributes(beforeScalaCode: ScalaCode, afterScalaCode: ScalaCode, scalaVersion: ScalaVersion)

object ComparisonAttributes extends ShapeTag.Companion[ComparisonAttributes] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "ComparisonAttributes")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(beforeScalaCode: ScalaCode, afterScalaCode: ScalaCode, scalaVersion: ScalaVersion): ComparisonAttributes = ComparisonAttributes(beforeScalaCode, afterScalaCode, scalaVersion)

  implicit val schema: Schema[ComparisonAttributes] = struct(
    ScalaCode.schema.required[ComparisonAttributes]("beforeScalaCode", _.beforeScalaCode),
    ScalaCode.schema.required[ComparisonAttributes]("afterScalaCode", _.afterScalaCode),
    ScalaVersion.schema.required[ComparisonAttributes]("scalaVersion", _.scalaVersion),
  )(make).withId(id).addHints(hints)
}
