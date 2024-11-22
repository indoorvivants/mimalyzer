package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class CreateComparisonOutput(comparisonId: ComparisonId, mimaProblems: Option[MimaProblems] = None, tastyMimaProblems: Option[TastyMimaProblems] = None)

object CreateComparisonOutput extends ShapeTag.Companion[CreateComparisonOutput] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "CreateComparisonOutput")

  val hints: Hints = Hints(
    smithy.api.Output(),
  ).lazily

  // constructor using the original order from the spec
  private def make(comparisonId: ComparisonId, mimaProblems: Option[MimaProblems], tastyMimaProblems: Option[TastyMimaProblems]): CreateComparisonOutput = CreateComparisonOutput(comparisonId, mimaProblems, tastyMimaProblems)

  implicit val schema: Schema[CreateComparisonOutput] = struct(
    ComparisonId.schema.required[CreateComparisonOutput]("comparisonId", _.comparisonId),
    MimaProblems.schema.optional[CreateComparisonOutput]("mimaProblems", _.mimaProblems),
    TastyMimaProblems.schema.optional[CreateComparisonOutput]("tastyMimaProblems", _.tastyMimaProblems),
  )(make).withId(id).addHints(hints)
}
