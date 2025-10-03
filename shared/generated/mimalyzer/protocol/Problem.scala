package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Problem(message: String, tag: Option[String] = None, symbol: Option[String] = None)

object Problem extends ShapeTag.Companion[Problem] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "Problem")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(tag: Option[String], symbol: Option[String], message: String): Problem = Problem(message, tag, symbol)

  implicit val schema: Schema[Problem] = struct(
    string.optional[Problem]("tag", _.tag),
    string.optional[Problem]("symbol", _.symbol),
    string.required[Problem]("message", _.message),
  )(make).withId(id).addHints(hints)
}
