package mimalyzer.frontend

import com.raquo.laminar.api.L.*

enum MsgType(val color: String):
  case Info
      extends MsgType(
        "msg-info"
      )
  case Error extends MsgType("msg-error")
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
