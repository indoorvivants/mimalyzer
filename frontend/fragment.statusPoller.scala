package mimalyzer.frontend

import com.raquo.laminar.api.L.*
import mimalyzer.protocol.ComparisonStatus
import mimalyzer.protocol.*, ProcessingStep.*
import com.raquo.waypoint.Router

def fragmentStatusPoller(
    pollingState: Var[State],
    readyState: Var[Option[ComparisonId]]
)(using Api, Router[Page]) =
  child <-- pollingState.signal
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
            "Analysis finished ",
            a("Permalink", navigateTo(Page.ComparisonPage(id)), basicLink)
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
            if label == CodeLabel.BEFORE then p("Code before failed to compile")
            else p("Code after failed to compile"),
            errorOut
          )
        )

      case State.Polling(_, Some(ComparisonStatus.NotFoundCase(_))) =>
        message(MsgType.Error, "Binding doesn't exist")

      case State.None => emptyNode
    }

def fragmentScalaPicker(scalaVersion: Var[String]) = div(
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
