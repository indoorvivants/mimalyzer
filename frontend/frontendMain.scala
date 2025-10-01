package mimalyzer.frontend

import fullstack_scala.protocol.*
import smithy4s_fetch.SimpleRestJsonFetchClient
import com.raquo.laminar.api.L.*
import scala.scalajs.js.Promise
import org.scalajs.dom
import fullstack_scala.protocol.CodeLabel.AFTER
import fullstack_scala.protocol.CodeLabel.BEFORE
import scalajs.js
import java.util.UUID
import fullstack_scala.protocol.ComparisonStatus.WaitingCase
import fullstack_scala.protocol.ProcessingStep.PICKED_UP
import fullstack_scala.protocol.ProcessingStep.CODE_BEFORE_COMPILED
import fullstack_scala.protocol.ProcessingStep.CODE_AFTER_COMPILED
import fullstack_scala.protocol.ProcessingStep.MIMA_FINISHED
import fullstack_scala.protocol.ProcessingStep.TASTY_MIMA_FINISHED

def createApiClient(uri: String) = SimpleRestJsonFetchClient(
  MimaService,
  uri
).make

extension [T](p: => Promise[T]) inline def stream = EventStream.fromJsPromise(p)

enum Action:
  case Submit

type JobId = UUID
enum State:
  case None
  case Fatal(msg: String)
  case Polling(id: ComparisonId, st: Option[ComparisonStatus])

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
    stateful("old-scala-code", "package hello\nclass X {def x: Int = ???}")

  val (newScalaCode, newSave) =
    stateful("new-scala-code", "package hello\nclass X {def y: Int = ???}")

  val (scalaVersion, versionSave) =
    stateful("scala-version-enum", ScalaVersion.SCALA_213.value)

  // val result = Var(Option.empty[Result])
  val actionBus = EventBus[Action]()

  val apiClient = createApiClient(dom.window.location.href)

  object ValidUUID:
    def unapply(u: String): Option[UUID] =
      try Some(UUID.fromString(u))
      catch case _ => None

  val page = dom.window.location.pathname match
    case s"/comparison/${ValidUUID(str)}" =>
      Some(ComparisonId(str))
    case _ => None

  val readyState = Var(page)
  val pollingState = Var(State.None)
  val updater = EventStream
    .periodic(intervalMs = 250)
    .withCurrentValueOf(pollingState.signal.debugSpy(fn => dom.console.log(fn)))
    .map(_._2)
    .collect {
      case State.Polling(id, None | Some(ComparisonStatus.WaitingCase(_))) => id
      case State.Polling(id, Some(ComparisonStatus.ProcessingCase(_)))     => id
    }
    .flatMapSwitch { id =>
      apiClient
        .getStatus(id)
        .stream
        .map(_.status)
        .map(status => State.Polling(id, Some(status)))
    } --> pollingState.writer

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

      // result.set(Some(Result.Waiting))

      given Stability = Stability()

      exponentialFetch(() => apiClient.createComparison(attributes))
        .`then`(
          good => pollingState.set(State.Polling(good.comparisonId, None)),
          bad =>
            bad match
              case e: CodeTooBig =>
                val lab = e.which match
                  case AFTER  => "New"
                  case BEFORE => "Old"

                pollingState.set(
                  State.Fatal(
                    s"$lab code too big: size [${e.sizeBytes}] is larger than allowed [${e.maxSizeBytes}]"
                  )
                )

              case err: smithy4s.http.UnknownErrorResponse =>
                pollingState.set(State.Fatal(s"server error: $err"))

              case other =>
                pollingState.set(State.Fatal(other.toString()))
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
      cls := "w-full border-2 border-slate-400 p-2 text-md",
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

  val basicLink =
    cls := "text-indigo-600 hover:no-underline underline"

  val app =
    div(
      updater,
      cls := "content mx-auto w-8/12 bg-white/70 p-6 rounded-xl max-w-screen-lg flex flex-col gap-4",
      div(
        h1("Mimalyzer", cls := "text-6xl"),
        p(
          "Check whether your code change is ",
          a(
            href := "https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html",
            "binary compatible",
            basicLink
          ),
          " in Scala according to ",
          a(
            href := "https://github.com/lightbend-labs/mima",
            "MiMa",
            basicLink
          ),
          " and TASTy compatible in Scala 3 according to ",
          a(
            href := "https://github.com/scalacenter/tasty-mima",
            "TASTy-MiMa",
            basicLink
          ),
          cls := "font-italic text-sm"
        )
      ),
      child <-- pollingState.signal
        .debugSpy(s => org.scalajs.dom.console.log(s.toString))
        .map {
          case State.Fatal(msg) =>
            message(MsgType.Error, msg)
          case State.Polling(_, None) =>
            message(MsgType.Info, "Submitted, waiting to hear back...")
          case State.Polling(_, Some(ComparisonStatus.WaitingCase(_))) =>
            message(MsgType.Info, "Job in queue, waiting to be picked up...")
          case State.Polling(_, Some(ComparisonStatus.ProcessingCase(p))) =>
            val ahead = p.remaining.fold("")(s => s"$s remaining")
            val step = p.step.map: p =>
              p match
                case PICKED_UP            => "picked up the job"
                case CODE_BEFORE_COMPILED => "scala code before compiled"
                case CODE_AFTER_COMPILED  => "scala code after compiled"
                case MIMA_FINISHED        => "MiMa finished"
                case TASTY_MIMA_FINISHED  => "TASTy MiMa finished"

            message(MsgType.Info, s"Working: ${step.getOrElse("")} $ahead")
          case State.Polling(id, Some(ComparisonStatus.CompletedCase(_))) =>
            readyState.set(Some(id))
            message(
              MsgType.Info,
              span(
                "Analysis finished "
              )
            )
          case State.Polling(
                _,
                Some(
                  ComparisonStatus
                    .FailedCase(CompilationFailed(label, errorOut))
                )
              ) =>
            message(
              MsgType.Error,
              div(
                if label == CodeLabel.BEFORE then
                  p("Code before failed to compile")
                else p("Code after failed to compile"),
                errorOut
              )
            )

          case State.Polling(_, Some(ComparisonStatus.NotFoundCase(_))) =>
            message(MsgType.Error, "Binding doesn't exist")

          case State.None => emptyNode
        },
      div(
        cls := "w-full flex flex-row gap-4",
        div(
          cls := "flex flex-col gap-2 grow-0 w-6/12",
          h2("Scala code before", cls := "font-bold"),
          p(
            "This simulates the previous version of your library",
            cls := "text-md"
          ),
          codeMirrorTextArea(oldScalaCode)
        ),
        div(
          cls := "flex flex-col gap-2 grow-0 w-6/12",
          h2("Scala code after", cls := "font-bold"),
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
            )
        )
      ),
      btn,
      pre(
        cls := "whitespace-pre-line rounded-md text-2xl p-4",
        child <-- readyState.signal.changes.collectSome.flatMapSwitch: id =>
          EventStream
            .fromJsPromise(apiClient.getComparison(id))
            .map: gso =>
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
      ),
      div(
        cls := "text-sm",
        "Made by ",
        a(
          href := "https://blog.indoorvivants.com",
          "Anton Sviridov",
          basicLink
        ),
        " using ",
        a(href := "https://scala-js.org", "Scala.js", basicLink),
        " and ",
        a(
          href := "https://disneystreaming.github.io/smithy4s/",
          "Smithy4s",
          basicLink
        ),
        " from ",
        a(
          href := "https://github.com/indoorvivants/scala-cli-smithy4s-fullstack-template",
          "Fullstack Scala Template",
          basicLink
        ),
        p(
          "Contribute on ",
          a(
            href := "https://github.com/keynmol/mimalyzer",
            "Github",
            basicLink
          )
        )
      ),
      handleEvents,
      saveState
    )

  renderOnDomContentLoaded(dom.document.getElementById("content"), app)
end hello

enum MsgType(val color: String):
  case Info
      extends MsgType(
        "bg-emerald-300 border-2 border-emerald-800 text-xl p-2 m-2"
      )
  case Error
      extends MsgType("bg-rose-300 border-2 border-rose-800  text-xl p-2 m-2")
  case Warn
      extends MsgType(
        "warn-message"
      )
end MsgType

def message(tpe: MsgType, value: Element) =
  div(
    cls := tpe.color,
    value
  )

def message(tpe: MsgType, value: String) =
  div(
    cls := tpe.color,
    value
  )
