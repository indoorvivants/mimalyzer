package mimalyzer.frontend

import mimalyzer.protocol.*
import smithy4s_fetch.SimpleRestJsonFetchClient
import com.raquo.laminar.api.L.*
import scala.scalajs.js.Promise
import org.scalajs.dom
import mimalyzer.protocol.CodeLabel.AFTER
import mimalyzer.protocol.CodeLabel.BEFORE
import scalajs.js
import java.util.UUID
import mimalyzer.protocol.ComparisonStatus.WaitingCase
import mimalyzer.protocol.ProcessingStep.PICKED_UP
import mimalyzer.protocol.ProcessingStep.CODE_BEFORE_COMPILED
import mimalyzer.protocol.ProcessingStep.CODE_AFTER_COMPILED
import mimalyzer.protocol.ProcessingStep.MIMA_FINISHED
import mimalyzer.protocol.ProcessingStep.TASTY_MIMA_FINISHED
import com.raquo.waypoint.*
import com.raquo.airstream.web.WebStorageVar

enum MsgType(val color: String):
  case Info
      extends MsgType(
        "bg-emerald-300 border-2 border-emerald-800 text-xl p-2 m-2"
      )
  case Error
      extends MsgType("bg-rose-300 border-2 border-rose-800  text-xl p-2 m-2")
  case Warn
      extends MsgType(
        "warn-message"
      )
end MsgType

def message(tpe: MsgType, value: Element) =
  div(
    cls := tpe.color,
    value
  )

def message(tpe: MsgType, value: String) =
  div(
    cls := tpe.color,
    value
  )
