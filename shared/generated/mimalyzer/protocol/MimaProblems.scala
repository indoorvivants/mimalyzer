package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class MimaProblems(problems: List[Problem])

object MimaProblems extends ShapeTag.Companion[MimaProblems] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "MimaProblems")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(problems: List[Problem]): MimaProblems = MimaProblems(problems)

  implicit val schema: Schema[MimaProblems] = struct(
    ProblemsList.underlyingSchema.required[MimaProblems]("problems", _.problems),
  )(make).withId(id).addHints(hints)
}
