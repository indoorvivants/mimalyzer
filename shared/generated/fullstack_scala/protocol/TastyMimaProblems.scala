package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class TastyMimaProblems(problems: List[Problem])

object TastyMimaProblems extends ShapeTag.Companion[TastyMimaProblems] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "TastyMimaProblems")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(problems: List[Problem]): TastyMimaProblems = TastyMimaProblems(problems)

  implicit val schema: Schema[TastyMimaProblems] = struct(
    ProblemsList.underlyingSchema.required[TastyMimaProblems]("problems", _.problems),
  )(make).withId(id).addHints(hints)
}
