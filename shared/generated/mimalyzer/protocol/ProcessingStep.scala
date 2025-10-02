package mimalyzer.protocol

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class ProcessingStep(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = ProcessingStep
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = ProcessingStep
  @inline final def widen: ProcessingStep = this
}
object ProcessingStep extends Enumeration[ProcessingStep] with ShapeTag.Companion[ProcessingStep] {
  val id: ShapeId = ShapeId("mimalyzer.protocol", "ProcessingStep")

  val hints: Hints = Hints.empty

  case object PICKED_UP extends ProcessingStep("picked-up", "PICKED_UP", 0, Hints.empty)
  case object CODE_BEFORE_COMPILED extends ProcessingStep("code-before-compiled", "CODE_BEFORE_COMPILED", 1, Hints.empty)
  case object CODE_AFTER_COMPILED extends ProcessingStep("code-after-compiled", "CODE_AFTER_COMPILED", 2, Hints.empty)
  case object MIMA_FINISHED extends ProcessingStep("mima-finished", "MIMA_FINISHED", 3, Hints.empty)
  case object TASTY_MIMA_FINISHED extends ProcessingStep("tasty-mima-finished", "TASTY_MIMA_FINISHED", 4, Hints.empty)

  val values: List[ProcessingStep] = List(
    PICKED_UP,
    CODE_BEFORE_COMPILED,
    CODE_AFTER_COMPILED,
    MIMA_FINISHED,
    TASTY_MIMA_FINISHED,
  )
  val tag: EnumTag[ProcessingStep] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[ProcessingStep] = enumeration(tag, values).withId(id).addHints(hints)
}
