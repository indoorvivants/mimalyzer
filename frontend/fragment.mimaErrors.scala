package mimalyzer.frontend

import com.raquo.airstream.core.EventStream
import mimalyzer.protocol.GetComparisonOutput
import com.raquo.laminar.api.L.*
import mimalyzer.frontend.MsgType
import mimalyzer.frontend.message
import mimalyzer.protocol.Problem
import mimalyzer.protocol.ScalaVersion

def fragmentMimaErrors(gso: EventStream[GetComparisonOutput]) =
  div(
    child <-- gso.map: gso =>
      val noProblems =
        gso.comparison.mimaProblems.isEmpty && gso.comparison.tastyMimaProblems.isEmpty
      if noProblems then
        div(
          message(
            MsgType.Info,
            "Congratulations! This change is binary compatible"
          ),
          Option.when(
            gso.comparison.attributes.scalaVersion == ScalaVersion.SCALA_3_LTS
          )(
            message(
              MsgType.Info,
              "Congratulations! This change is TASTy compatible"
            )
          )
        )
      else
        def renderProblems(pl: List[Problem]) =
          ul(
            cls := "m-4 pl-4 text-small",
            pl.map(p => li(cls := "pl-2 list-disc", p.message))
          )

        div(
          gso.comparison.mimaProblems.map: mima =>
            message(
              MsgType.Error,
              div(
                p(
                  "This change is not binary compatible according to MiMa"
                ),
                renderProblems(mima.problems)
              )
            ),
          gso.comparison.tastyMimaProblems.map: mima =>
            message(
              MsgType.Error,
              div(
                p(
                  "This change is not TASTy compatible according to Tasty-MiMa"
                ),
                renderProblems(mima.problems)
              )
            )
        )
      end if
  )
