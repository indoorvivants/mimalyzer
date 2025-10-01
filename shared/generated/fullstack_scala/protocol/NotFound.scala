package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.constant

final case class NotFound() extends Smithy4sThrowable

object NotFound extends ShapeTag.Companion[NotFound] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "NotFound")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(400),
  ).lazily


  implicit val schema: Schema[NotFound] = constant(NotFound()).withId(id).addHints(hints)
}
