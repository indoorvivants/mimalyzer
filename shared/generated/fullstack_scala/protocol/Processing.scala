package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class Processing(step: Option[ProcessingStep] = None, remaining: Option[Int] = None)

object Processing extends ShapeTag.Companion[Processing] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "Processing")

  val hints: Hints = Hints.empty

  // constructor using the original order from the spec
  private def make(step: Option[ProcessingStep], remaining: Option[Int]): Processing = Processing(step, remaining)

  implicit val schema: Schema[Processing] = struct(
    ProcessingStep.schema.optional[Processing]("step", _.step),
    int.optional[Processing]("remaining", _.remaining),
  )(make).withId(id).addHints(hints)
}
