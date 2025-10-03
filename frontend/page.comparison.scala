package mimalyzer.frontend

import mimalyzer.protocol.*
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

def renderComparisonPage(
    beforeCodeVar: Var[String],
    afterCodeVar: Var[String],
    scalaVersionVar: Var[String],
    signal: Signal[ComparisonId]
)(using Api, Router[Page]) =
  val sig = signal.flatMapSwitch(cid => Api.client.stream(_.getComparison(cid)))
  fragmentLayout(
    div(
      cls := "comparison-page",
      fragmentStaticScalaSnippets(
        beforeCodeVar.set(_),
        afterCodeVar.set(_),
        scalaVersionVar.set(_),
        sig
      ),
      fragmentMimaErrors(
        sig
      )
    )
  )
end renderComparisonPage
