package mimalyzer.frontend

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

def fragmentHeader(using Router[Page]) = div(
  h1(a("Mimalyzer", cls := "main-title", navigateTo(Page.Main))),
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
    cls := "header-description"
  )
)
