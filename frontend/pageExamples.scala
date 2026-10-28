package mimalyzer.frontend

import mimalyzer.protocol.*
import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router
import scala.util.matching.Regex

def toRegex(s: String): Regex =
  s match
    case "*"   => ".*".r
    case other => other.split("*").mkString(".*").r

def renderExamplesPage(
    oldScalaCode: Var[String],
    newScalaCode: Var[String],
    scalaVersion: Var[String],
    actionBus: EventBus[Action],
    scalaVersions: Signal[List[String]]
)(using Api, Router[Page]) =

  fragmentLayout(
    div(
      cls := "examples-page",
      h1("Examples"),
      navTag(
        cls := "examples-toc",
        ul(
          snippets.map: (name, snippet) =>
            li(a(href := s"#${name}", snippet.description, basicLink))
        )
      ),
      snippets.map: (name, snippet) =>
        val pollingState = Var(State.None)
        val readyState = Var(Option.empty[ComparisonId])
        val selectedScala = Var(Option.empty[String])
        div(
          cls := "example-card",
          h2(
            a(href := s"#${name}", "#"),
            snippet.description,
            cls := "example-header",
            idAttr := name
          ),
          div(
            cls := "snippet-example-code",
            div(
              cls := "compatibility-badges",
              span(
                cls := s"compatibile-badge-${snippet.bincompat}",
                "Binary compatible"
              ),
              span(
                cls := s"compatibile-badge-${snippet.sourcecompat}",
                "Source compatible"
              )
            ),
            p(
              cls := "try-it-row",
              "Try it:",
              children <-- scalaVersions
                .map(
                  _.filter(ver => toRegex(snippet.scala).matches(ver))
                )
                .map { versions =>
                  versions.map { ver =>
                    val label =
                      if ver.startsWith("3.3") || ver.startsWith("3.9")
                      then ver + " (LTS)"
                      else if ver.startsWith("3.10") then ver + " (Next)"
                      else ver

                    button(
                      label,
                      cls <-- selectedScala.signal.map:
                        case Some(`ver`) => "scala-button-selected"
                        case _           => "scala-button"
                      ,
                      onClick --> { _ =>
                        val attributes = ComparisonAttributes(
                          ScalaCode(snippet.before),
                          ScalaCode(snippet.after),
                          scalaVersion = ScalaVersion(ver)
                        )

                        selectedScala.set(Some(ver))

                        given Stability = Stability()

                        readyState.set(None)

                        submitComparison(attributes, pollingState)
                      }
                    )
                  }
                }
            ),
            fragmentStatusPoller(pollingState, readyState),
            poller(pollingState),
            div(
              cls := "snippet-code-row",
              div(
                cls := "snippet-code-column",
                p("Before", cls := "snippet-code-title"),
                codeBlock(
                  "scala",
                  snippet.before
                )
              ),
              div(
                cls := "snippet-code-column",
                p("After", cls := "snippet-code-title"),
                codeBlock(
                  "scala",
                  snippet.after
                )
              )
            ),
            fragmentMimaErrors(
              readyState.signal.changes.collectSome.flatMapSwitch(id =>
                Api.client.stream(_.getComparison(id))
              )
            )
          )
        )
    )
  )
