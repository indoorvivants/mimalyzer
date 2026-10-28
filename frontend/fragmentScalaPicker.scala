package mimalyzer.frontend

import mimalyzer.protocol.*
import com.raquo.laminar.api.L.*

def fragmentScalaPicker(
    scalaVersion: Var[String],
    availableVersions: Signal[List[ScalaVersion]]
)(using Api) = div(
  cls := "scala-picker-container",
  children <-- availableVersions.map { scalaVersions =>
    scalaVersions
      .sortBy(sv =>
        val parts = sv.value.split('.'); (parts(0).toInt, parts(1).toInt)
      )
      .reverse
      .map { sv =>
        p(
          cls := "scala-version-option",
          cls("scala-version-selected") <-- scalaVersion.signal.map(
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
          p(
            "Scala ",
            if sv.value.startsWith("3.3") || sv.value.startsWith("3.9") then
              span(sv.value, strong(" (LTS)"))
            else if sv.value.startsWith("3.10") then
              span(sv.value, strong(" (Next)"))
            else span(sv.value)
          )
        )
      }
  }
)
