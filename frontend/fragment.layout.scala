package mimalyzer.frontend

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.Router

def fragmentLayout(el: Element)(using Router[Page]) =
  div(
    cls := "content mx-auto w-8/12 bg-white/70 p-6 rounded-xl max-w-screen-lg flex flex-col gap-4",
    fragmentHeader,
    el,
    footerFragment
  )
