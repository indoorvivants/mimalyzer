package mimalyzer.frontend

import com.raquo.airstream.core.EventStream
import mimalyzer.protocol.GetComparisonOutput
import com.raquo.laminar.api.L.*
import mimalyzer.frontend.MsgType
import mimalyzer.frontend.message
import mimalyzer.protocol.Problem
import mimalyzer.protocol.ScalaVersion
import mimalyzer.protocol.isScala3

def fragmentMimaErrors(gso: EventStream[GetComparisonOutput]) =
  div(
    child <-- gso.map: gso =>
      // val noProblems =
      //   gso.comparison.mimaProblems.isEmpty && gso.comparison.tastyMimaProblems.isEmpty
      val mimaBlock =
        if gso.comparison.mimaProblems.isEmpty then
          message(
            MsgType.Info,
            "Congratulations! This change is binary compatible"
          )
        else
          div(
            cls := "error-container",
            gso.comparison.mimaProblems.map: mima =>
              message(
                MsgType.Error,
                div(
                  p(
                    "This change is not binary compatible according to MiMa"
                  ),
                  renderProblems(mima.problems)
                )
              )
          )

      val tastyMimaBlock =
        Option.when(isScala3(gso.comparison.attributes.scalaVersion))(
          if gso.comparison.tastyMimaProblems.isEmpty then
            message(
              MsgType.Info,
              "Congratulations! This change is TASTy compatible"
            )
          else
            div(
              cls := "error-container",
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
        )

      div(mimaBlock, tastyMimaBlock)
  )

private def renderProblems(pl: List[Problem]) =
  ul(
    cls := "error-list",
    pl.map(problem =>
      val details =
        if problem.tag.nonEmpty || problem.symbol.nonEmpty then
          p(
            cls := "error-details",
            problem.tag.map(t => span(cls := "error-tag", t)),
            problem.symbol.map(t => span(cls := "error-symbol", t))
          )
        else emptyNode
      li(
        cls := "error-list-item",
        details,
        p(cls := "error-message", problem.message)
      )
    )
  )
