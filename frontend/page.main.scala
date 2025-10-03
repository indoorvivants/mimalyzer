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
      cls := "submit-button"
    )

  div(
    poller,
    cls := "main-content-container",
    fragmentHeader,
    fragmentStatusPoller(pollingState, readyState),
    div(
      cls := "code-input-row",
      div(
        cls := "code-input-column",
        h2("Scala code before", cls := "code-input-header"),
        p(
          "This simulates the previous version of your library",
          cls := "code-input-description"
        ),
        codeMirrorTextArea(oldScalaCode)
      ),
      div(
        cls := "code-input-column",
        h2("Scala code after", cls := "code-input-header"),
        p(
          "This simulates the next version of your library",
          cls := "code-input-description"
        ),
        codeMirrorTextArea(newScalaCode)
      )
    ),
    fragmentScalaPicker(scalaVersion),
    submitButton,
    fragmentMimaErrors(
      readyState.signal.changes.collectSome.flatMapSwitch(id =>
        Api.client.stream(_.getComparison(id))
      )
    ),
    footerFragment,
    handleFormSubmit
  )
end renderMainPage

def codeMirrorTextArea(target: Var[String]) =
  textArea(
    cls := "code-textarea",
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
