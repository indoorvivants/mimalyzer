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

enum State:
  case None
  case Fatal(msg: String)
  case Polling(id: ComparisonId, st: Option[ComparisonStatus])