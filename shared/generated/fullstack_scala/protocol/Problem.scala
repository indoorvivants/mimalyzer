package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class Problem(message: String)

object Problem extends ShapeTag.Companion[Problem] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "Problem")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(message: String): Problem = Problem(message)

  implicit val schema: Schema[Problem] = struct(
    string.required[Problem]("message", _.message),
  )(make).withId(id).addHints(hints)
}
