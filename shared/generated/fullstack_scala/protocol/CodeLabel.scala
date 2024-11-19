package fullstack_scala.protocol

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class CodeLabel(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = CodeLabel
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = CodeLabel
  @inline final def widen: CodeLabel = this
}
object CodeLabel extends Enumeration[CodeLabel] with ShapeTag.Companion[CodeLabel] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "CodeLabel")

  val hints: Hints = Hints.empty

  case object AFTER extends CodeLabel("AFTER", "AFTER", 0, Hints.empty)
  case object BEFORE extends CodeLabel("BEFORE", "BEFORE", 1, Hints.empty)

  val values: List[CodeLabel] = List(
    AFTER,
    BEFORE,
  )
  val tag: EnumTag[CodeLabel] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[CodeLabel] = enumeration(tag, values).withId(id).addHints(hints)
}
