package mimalyzer.frontend

import com.raquo.laminar.api.L.*

val basicLink =
  cls := "basic-link"

val footerFragment =
  div(
    cls := "footer-container",
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
  )
