package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.Smithy4sThrowable
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

final case class CompilationFailed(which: CodeLabel, errorOut: String) extends Smithy4sThrowable

object CompilationFailed extends ShapeTag.Companion[CompilationFailed] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "CompilationFailed")

  val hints: Hints = Hints(
    smithy.api.Error.CLIENT.widen,
    smithy.api.HttpError(400),
  ).lazily

  // constructor using the original order from the spec
  private def make(which: CodeLabel, errorOut: String): CompilationFailed = CompilationFailed(which, errorOut)

  implicit val schema: Schema[CompilationFailed] = struct(
    CodeLabel.schema.required[CompilationFailed]("which", _.which),
    string.required[CompilationFailed]("errorOut", _.errorOut),
  )(make).withId(id).addHints(hints)
}
