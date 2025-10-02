package mimalyzer.frontend

import com.raquo.laminar.api.L.*

val basicLink =
  cls := "text-indigo-600 hover:no-underline underline"

val footerFragment =
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
  )
