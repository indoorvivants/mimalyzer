package mimalyzer.frontend

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import org.scalajs.dom

import java.util.UUID
import com.raquo.airstream.web.WebStorageVar
import mimalyzer.protocol.ScalaVersion

type JobId = UUID

@main def mimalyzerFrontendMain =
  given Api = Api.create()
  given Router[Page] = router

  val oldScalaCode = WebStorageVar
    .localStorage(
      key = "mimalyzer-old-scala-code",
      syncOwner = Some(unsafeWindowOwner)
    )
    .text("package hello\nclass X {def x: Int = ???}")

  val newScalaCode = WebStorageVar
    .localStorage(
      key = "mimalyzer-new-scala-code",
      syncOwner = Some(unsafeWindowOwner)
    )
    .text("package hello\nclass X {def y: Int = ???}")

  val scalaVersion = WebStorageVar
    .localStorage(
      key = "mimalyzer-scala-version",
      syncOwner = Some(unsafeWindowOwner)
    )
    .text(ScalaVersion.SCALA_213.value)

  val app = MimalyzerFrontend(oldScalaCode, newScalaCode, scalaVersion)

  renderOnDomContentLoaded(
    dom.document.getElementById("content"),
    div(child <-- app.signal)
  )
end mimalyzerFrontendMain

class MimalyzerFrontend(
    oldCodeVar: Var[String],
    newCodeVar: Var[String],
    scalaVersionVar: Var[String]
)(using api: Api, router: Router[Page]):
  val signal = SplitRender[Page, HtmlElement](router.currentPageSignal)
    .collectSignal[Page.ComparisonPage] { userPageSignal =>
      renderComparisonPage(
        oldCodeVar,
        newCodeVar,
        scalaVersionVar,
        userPageSignal.map(_.id)
      )
    }
    .collectStatic(Page.Main) {
      renderMainPage(oldCodeVar, newCodeVar, scalaVersionVar)
    }
    .signal
end MimalyzerFrontend
