package mimalyzer.frontend

import mimalyzer.protocol.*
import com.raquo.laminar.api.L.*, com.raquo.waypoint.*
import com.raquo.airstream.web.WebStorageVar
import mimalyzer.protocol.CodeLabel.*
import scalajs.js, org.scalajs.dom

def renderMainPage(
    oldScalaCode: Var[String],
    newScalaCode: Var[String],
    scalaVersion: Var[String]
)(using
    Api,
    Router[Page]
) =
  val actionBus = EventBus[Action]()
  val readyState = Var(Option.empty[ComparisonId])
  val pollingState = Var(State.None)

  val poller = EventStream
    .periodic(intervalMs = 250)
    .withCurrentValueOf(pollingState.signal.debugSpy(fn => dom.console.log(fn)))
    .map(_._2)
    .collect {
      case State.Polling(id, None | Some(ComparisonStatus.WaitingCase(_))) => id
      case State.Polling(id, Some(ComparisonStatus.ProcessingCase(_)))     => id
    }
    .flatMapSwitch { id =>
      Api.client
        .stream(_.getStatus(id))
        .map(_.status)
        .map(status => State.Polling(id, Some(status)))
    } --> pollingState.writer

  val handleFormSubmit =
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

      given Stability = Stability()

      exponentialFetch(() => Api.client.promise(_.createComparison(attributes)))
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

  val submitButton =
    button(
      "Check it",
      onClick.mapToStrict(Action.Submit) --> actionBus,
      cls := "bg-sky-700 text-lg font-bold p-2 text-white"
    )

  div(
    poller,
    cls := "content mx-auto w-8/12 bg-white/70 p-6 rounded-xl max-w-screen-lg flex flex-col gap-4",
    fragmentHeader,
    fragmentStatusPoller(pollingState, readyState),
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
    fragmentScalaPicker(scalaVersion),
    submitButton,
    pre(
      cls := "whitespace-pre-line rounded-md text-2xl p-4",
      fragmentMimaErrors(
        readyState.signal.changes.collectSome.flatMapSwitch(id =>
          Api.client.stream(_.getComparison(id))
        )
      )
    ),
    footerFragment,
    handleFormSubmit
  )
end renderMainPage

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
