package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.int
import smithy4s.schema.Schema.struct

final case class CodeTooBig(sizeBytes: Int, maxSizeBytes: Int, which: CodeLabel) extends Smithy4sThrowable {
}

object CodeTooBig extends ShapeTag.Companion[CodeTooBig] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "CodeTooBig")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(400),
  ).lazily

  // constructor using the original order from the spec
  private def make(sizeBytes: Int, maxSizeBytes: Int, which: CodeLabel): CodeTooBig = CodeTooBig(sizeBytes, maxSizeBytes, which)

  implicit val schema: Schema[CodeTooBig] = struct(
    int.required[CodeTooBig]("sizeBytes", _.sizeBytes),
    int.required[CodeTooBig]("maxSizeBytes", _.maxSizeBytes),
    CodeLabel.schema.required[CodeTooBig]("which", _.which),
  )(make).withId(id).addHints(hints)
}
