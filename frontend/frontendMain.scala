package fullstack_scala
package frontend

import fullstack_scala.protocol.*
import smithy4s_fetch.SimpleRestJsonFetchClient
import com.raquo.laminar.api.L.*
import scala.scalajs.js.Promise
import org.scalajs.dom

def createApiClient(uri: String) = SimpleRestJsonFetchClient(
  MimaService,
  uri
).make

extension [T](p: => Promise[T]) inline def stream = EventStream.fromJsPromise(p)

enum Action:
  case Submit

@main def hello =
  // State
  val oldScalaCode = Var("class X{val x: Int = ???}")
  val newScalaCode = Var("class X{def y: Int = ???}")
  val scalaVersion = Var("2.13.15")
  val result = Var("")
  val actionBus = EventBus[Action]()
  val inprogress = Var(false)

  val apiClient = createApiClient(dom.window.location.href)

  val handleEvents =
    actionBus.events.withCurrentValueOf(
      oldScalaCode,
      newScalaCode,
      scalaVersion
    ) --> { case (Action.Submit, old, nw, sv) =>
      val attributes =
        ComparisonAttributes(
          beforeScalaCode = ScalaCode(old),
          afterScalaCode = ScalaCode(nw),
          scalaVersion = ScalaVersion(sv)
        )

      result.set("WAITING....")

      apiClient
        .createComparison(attributes)
        .`then`(
          good =>
            result.set(
              if good.problems.isEmpty then "NO PROBLEMS"
              else "PROBLEMS\n" + good.problems.map(_.toString).mkString("\n")
            ),
          bad => result.set(s"ERROR: $bad")
        )

    }


  val btn =
    button(
      "Check it",
      onClick.mapToStrict(Action.Submit) --> actionBus,
      cls := "bg-sky-700 text-lg font-bold p-2 text-white"
    )

  val app =
    div(
      cls := "content mx-auto w-6/12 rounded-lg border-2 border-slate-400 p-4",
      p("Mimalyzer", cls := "text-2xl m-2"),
      div(
        cls := "flex gap-4",
        img(
          src := "https://www.scala-lang.org/resources/img/frontpage/scala-spiral.png",
          cls := "w-24"
        ),
        div(
          cls := "w-full",
          h2("Old Scala code"),
          textArea(
            cls := "w-full border-2 border-slate-400",
            onInput.mapToValue --> oldScalaCode,
            value <-- oldScalaCode
          ),
          h2("New Scala code"),
          textArea(
            cls := "w-full border-2 border-slate-400",
            onInput.mapToValue --> newScalaCode,
            value <-- newScalaCode
          ),
          h2("Scala version"),
          input(
            cls := "w-full border-2 border-slate-400",
            onInput.mapToValue --> scalaVersion,
            value <-- scalaVersion
          ),
          btn,
          pre(code(child.text <-- result)),
          handleEvents
        )
      )
    )

  renderOnDomContentLoaded(dom.document.getElementById("content"), app)
end hello
