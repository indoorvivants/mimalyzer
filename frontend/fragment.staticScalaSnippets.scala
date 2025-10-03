package mimalyzer.frontend

import com.raquo.airstream.core.EventStream
import com.raquo.laminar.api.L.*
import mimalyzer.protocol.GetComparisonOutput
import com.raquo.waypoint.Router

def fragmentStaticScalaSnippets(
    setCodeBefore: String => Unit,
    setCodeAfter: String => Unit,
    setScalaVersion: String => Unit,
    gso: EventStream[GetComparisonOutput]
)(using Router[Page]) =
  div(
    child <-- gso.map: gso =>
      div(
        div(
          cls := "snippet-controls",
          a(
            "⋔ Fork",
            href := "#",
            basicLink,
            onClick --> { _ =>
              setScalaVersion(gso.comparison.attributes.scalaVersion.value)
              setCodeBefore(gso.comparison.attributes.beforeScalaCode.value)
              setCodeAfter(gso.comparison.attributes.afterScalaCode.value)
              redirectTo(Page.Main)
            }
          ),
          a(
            "📋 Copy permalink",
            href := "#",
            basicLink,
            onClick --> { _ =>
              org.scalajs.dom.window.navigator.clipboard
                .writeText(org.scalajs.dom.window.location.href)
            }
          )
        ),
        p(
          cls := "snippet-version-display",
          "Scala version " + gso.comparison.attributes.scalaVersion.value
        ),
        div(
          cls := "snippet-code-row",
          div(
            cls := "snippet-code-column",
            p("Scala code before", cls := "snippet-code-title"),
            codeBlock("scala", gso.comparison.attributes.beforeScalaCode.value)
          ),
          div(
            cls := "snippet-code-column",
            p("Scala code after", cls := "snippet-code-title"),
            codeBlock("scala", gso.comparison.attributes.afterScalaCode.value)
          )
        )
      )
  )
