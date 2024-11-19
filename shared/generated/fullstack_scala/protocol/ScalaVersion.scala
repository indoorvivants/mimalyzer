package fullstack_scala.protocol

import smithy4s.Enumeration
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.EnumTag
import smithy4s.schema.Schema.enumeration

sealed abstract class ScalaVersion(_value: String, _name: String, _intValue: Int, _hints: Hints) extends Enumeration.Value {
  override type EnumType = ScalaVersion
  override val value: String = _value
  override val name: String = _name
  override val intValue: Int = _intValue
  override val hints: Hints = _hints
  override def enumeration: Enumeration[EnumType] = ScalaVersion
  @inline final def widen: ScalaVersion = this
}
object ScalaVersion extends Enumeration[ScalaVersion] with ShapeTag.Companion[ScalaVersion] {
  val id: ShapeId = ShapeId("fullstack_scala.protocol", "ScalaVersion")

  val hints: Hints = Hints.empty

  case object SCALA_212 extends ScalaVersion("2.12", "SCALA_212", 0, Hints.empty)
  case object SCALA_213 extends ScalaVersion("2.13", "SCALA_213", 1, Hints.empty)
  case object SCALA_3_LTS extends ScalaVersion("3 LTS", "SCALA_3_LTS", 2, Hints.empty)

  val values: List[ScalaVersion] = List(
    SCALA_212,
    SCALA_213,
    SCALA_3_LTS,
  )
  val tag: EnumTag[ScalaVersion] = EnumTag.ClosedStringEnum
  implicit val schema: Schema[ScalaVersion] = enumeration(tag, values).withId(id).addHints(hints)
}
