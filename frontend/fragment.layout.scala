package mimalyzer.frontend

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

def fragmentLayout(el: Element)(using Router[Page]) =
  div(
    cls := "main-content-container",
    fragmentHeader,
    el,
    footerFragment
  )
