package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.struct

final case class Comparison(id: ComparisonId, attributes: ComparisonAttributes, mimaProblems: Option[MimaProblems] = None, tastyMimaProblems: Option[TastyMimaProblems] = None)

object Comparison extends ShapeTag.Companion[Comparison] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "Comparison")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(id: ComparisonId, attributes: ComparisonAttributes, mimaProblems: Option[MimaProblems], tastyMimaProblems: Option[TastyMimaProblems]): Comparison = Comparison(id, attributes, mimaProblems, tastyMimaProblems)

  implicit val schema: Schema[Comparison] = struct(
    ComparisonId.schema.required[Comparison]("id", _.id),
    ComparisonAttributes.schema.required[Comparison]("attributes", _.attributes),
    MimaProblems.schema.optional[Comparison]("mimaProblems", _.mimaProblems),
    TastyMimaProblems.schema.optional[Comparison]("tastyMimaProblems", _.tastyMimaProblems),
  )(make).withId(id).addHints(hints)
}
