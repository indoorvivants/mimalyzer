package mimalyzer.protocol

import smithy4s.Hints
import smithy4s.Newtype
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.list

object ProblemsList extends Newtype[List[Problem]] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "ProblemsList")
  val hints: Hints = Hints.empty
  val underlyingSchema: Schema[List[Problem]] = list(Problem.schema).withId(id).addHints(hints)
  implicit val schema: Schema[ProblemsList] = bijection(underlyingSchema, asBijection)
}
