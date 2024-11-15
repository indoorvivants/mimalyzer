package fullstack_scala
package frontend

import fullstack_scala.protocol.*
import smithy4s_fetch.SimpleRestJsonFetchClient
import com.raquo.laminar.api.L.*
import scala.scalajs.js.Promise
import org.scalajs.dom
import fullstack_scala.protocol.CodeLabel.AFTER
import fullstack_scala.protocol.CodeLabel.BEFORE

def createApiClient(uri: String) = SimpleRestJsonFetchClient(
  MimaService,
  uri
).make

extension [T](p: => Promise[T]) inline def stream = EventStream.fromJsPromise(p)

enum Action:
  case Submit

def stateful(key: String, default: String) =
  val v = Var(
    Option(dom.window.localStorage.getItem(key))
      .getOrElse(default)
  )

  val b = v --> { value =>
    dom.window.localStorage.setItem(key, value)
  }

  v -> b
end stateful

@main def hello =
  val (oldScalaCode, oldSave) =
    stateful("old-scala-code", "class X {def x: Int = ???}")

  val (newScalaCode, newSave) =
    stateful("new-scala-code", "class X {def y: Int = ???}")

  val (scalaVersion, versionSave) = stateful("scala-version", "2.13.15")

  val result = Var("")
  val actionBus = EventBus[Action]()

  val apiClient = createApiClient(dom.window.location.href)

  val saveState =
    Seq(
      oldSave,
      newSave,
      versionSave
    )

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
              else
                "PROBLEMS\n" + good.problems
                  .flatMap(_.message)
                  .map("- " + _)
                  .mkString("\n")
            ),
          bad =>
            bad match
              case e: CodeTooBig =>
                val lab = e.which match
                  case AFTER  => "New"
                  case BEFORE => "Old"

                result.set(
                  s"$lab code too big: size [${e.sizeBytes}] is larger than allowed [${e.maxSizeBytes}]"
                )

              case _: InvalidScalaVersion =>
                result.set("Invalid Scala version")

              case e: CompilationFailed => 
                val lab = e.which match
                  case AFTER  => "New"
                  case BEFORE => "Old"

                result.set(s"$lab code failed to compile:\n\n${e.errorOut}")


              case other =>
                result.set(s"ERROR: $bad")
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
      cls := "content mx-auto w-8/12",
      p("Mimalyzer", cls := "text-2xl m-2"),
      div(
        cls := "flex gap-4",
        img(
          src := "https://www.scala-lang.org/resources/img/frontpage/scala-spiral.png",
          cls := "w-24"
        ),
        div(
          cls := "w-full",
          h2("Old Scala code", cls := "font-bold"),
          textArea(
            cls := "w-full border-2 border-slate-400",
            onInput.mapToValue --> oldScalaCode,
            value <-- oldScalaCode
          ),
          h2("New Scala code", cls := "font-bold"),
          textArea(
            cls := "w-full border-2 border-slate-400",
            onInput.mapToValue --> newScalaCode,
            value <-- newScalaCode
          ),
          h2("Scala version", cls := "font-bold"),
          input(
            cls := "w-full border-2 border-slate-400",
            onInput.mapToValue --> scalaVersion,
            value <-- scalaVersion
          ),
          btn,
          pre(cls := "whitespace-pre-line", code(child.text <-- result)),
          handleEvents,
          saveState
        )
      )
    )

  renderOnDomContentLoaded(dom.document.getElementById("content"), app)
end hello
