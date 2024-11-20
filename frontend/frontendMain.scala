package mimalyzer.frontend

import fullstack_scala.protocol.*
import smithy4s_fetch.SimpleRestJsonFetchClient
import com.raquo.laminar.api.L.*
import scala.scalajs.js.Promise
import org.scalajs.dom
import fullstack_scala.protocol.CodeLabel.AFTER
import fullstack_scala.protocol.CodeLabel.BEFORE
import scalajs.js

def createApiClient(uri: String) = SimpleRestJsonFetchClient(
  MimaService,
  uri
).make

extension [T](p: => Promise[T]) inline def stream = EventStream.fromJsPromise(p)

enum Action:
  case Submit

enum Result:
  case NoProblems
  case Problems(s: String)
  case Error(msg: String)
  case Waiting

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

  val (scalaVersion, versionSave) =
    stateful("scala-version-enum", ScalaVersion.SCALA_213.value)

  val result = Var(Option.empty[Result])
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
          scalaVersion = ScalaVersion.values.find(_.value == sv).get
        )

      result.set(Some(Result.Waiting))

      apiClient
        .createComparison(attributes)
        .`then`(
          good =>
            result.set(
              Option(
                if good.problems.isEmpty then Result.NoProblems
                else
                  Result.Problems(
                    good.problems
                      .flatMap(_.message)
                      .map("- " + _)
                      .mkString("\n")
                  )
              )
            ),
          bad =>
            bad match
              case e: CodeTooBig =>
                val lab = e.which match
                  case AFTER  => "New"
                  case BEFORE => "Old"

                result.set(
                  Option(
                    Result.Error(
                      s"$lab code too big: size [${e.sizeBytes}] is larger than allowed [${e.maxSizeBytes}]"
                    )
                  )
                )

              case err: smithy4s.http.UnknownErrorResponse =>
                if err.code == 502 then
                  result.set(
                    Option(
                      Result.Error(
                        "Looks like Fly.io killed the server - wait for a couple seconds and press the button again"
                      )
                    )
                  )

              case e: CompilationFailed =>
                val lab = e.which match
                  case AFTER  => "New"
                  case BEFORE => "Old"

                result.set(
                  Option(
                    Result
                      .Error(s"$lab code failed to compile:\n\n${e.errorOut}")
                  )
                )

              case other =>
                result.set(Option(Result.Error(other.toString())))
        )

    }

  val btn =
    button(
      "Check it",
      onClick.mapToStrict(Action.Submit) --> actionBus,
      cls := "bg-sky-700 text-lg font-bold p-2 text-white"
    )

  def codeMirrorTextArea(target: Var[String]) =
    textArea(
      cls := "w-full border-2 border-slate-400 p-2",
      onInput.mapToValue --> target,
      value <-- target,
      onMountCallback(el =>
        CodeMirror
          .fromTextArea(
            el.thisNode.ref,
            js.Dictionary(
              "value" -> target.now(),
              "lineNumbers" -> true,
              "mode" -> "text/x-scala"
            )
          )
          .on("change", value => target.set(value.getValue()))
      )
    )

  val app =
    div(
      cls := "content mx-auto w-8/12 bg-white/70 p-6 rounded-xl max-w-screen-lg flex flex-col gap-4",
      div(
        h1("Mimalyzer", cls := "text-6xl"),
        p(
          "Check whether your code change is binary compatible in Scala, according to MiMa",
          cls := "font-italic text-sm"
        )
      ),
      div(
        cls := "w-full flex flex-row gap-4",
        div(
          cls := "w-full flex flex-col gap-2",
          h2("Old Scala code", cls := "font-bold"),
          p(
            "This simulates the previous version of your library",
            cls := "text-md"
          ),
          codeMirrorTextArea(oldScalaCode)
        ),
        div(
          cls := "w-full flex flex-col gap-2",
          h2("New Scala code", cls := "font-bold"),
          p(
            "This simulates the next version of your library",
            cls := "text-md"
          ),
          codeMirrorTextArea(newScalaCode)
        )
      ),
      div(
        div(
          cls := "flex flex-row gap-8 text-2xl place-content-center",
          ScalaVersion.values.map: sv =>
            p(
              cls := "flex flex-row gap-2 m-2 border-2 border-slate-200 p-4 rounded-md cursor-pointer",
              cls("bg-rose-700 text-white") <-- scalaVersion.signal.map(
                _ == sv.value
              ),
              onClick.mapTo(sv.value) --> scalaVersion,
              input(
                tpe := "radio",
                nameAttr := "scala-version",
                value := sv.value,
                checked <-- scalaVersion.signal.map(_ == sv.value),
                onChange.mapToValue --> scalaVersion
              ),
              p("Scala ", sv.value)
            ),
        )
      ),
      btn,
      pre(
        cls := "whitespace-pre-line rounded-md text-2xl p-4",
        cls("bg-emerald-400") <-- result.signal.map:
          case Some(Result.NoProblems) => true
          case _                       => false,
        cls("bg-rose-400") <-- result.signal.map:
          case Some(Result.Problems(_)) => true
          case _                        => false,
        cls("bg-amber-400") <-- result.signal.map:
          case Some(Result.Error(_)) => true
          case _                     => false,
        child <-- result.signal.map:
          case Some(Result.NoProblems) =>
            span("Congratulations! This change is binary compatible")
          case Some(Result.Problems(s)) =>
            p(
              p(
                "DANGER!\n This change is not binary compatible. Here's what MiMa reports:",
                cls := "font-bold"
              ),
              s
            )
          case Some(Result.Error(s)) =>
            p(
              p(
                "Something is wrong",
                cls := "font-bold"
              ),
              s
            )

          case Some(Result.Waiting) => i("Please wait...")
          case None                 => "",
        display <-- result.signal.map(s =>
          if s.nonEmpty then display.block.value else display.none.value
        )
      ),
      handleEvents,
      saveState
    )

  renderOnDomContentLoaded(dom.document.getElementById("content"), app)
end hello
