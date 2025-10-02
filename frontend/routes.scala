package mimalyzer.frontend

import com.raquo.laminar.api.L.*
import com.raquo.waypoint.*
import mimalyzer.protocol.*
import java.util.UUID

sealed trait Page extends Product with Serializable
object Page:
  case class ComparisonPage(id: ComparisonId) extends Page
  case object Main extends Page

val bindingPageRoute = Route(
  encode = (bindingPage: Page.ComparisonPage) => bindingPage.id.value.toString,
  decode = arg => Page.ComparisonPage(id = ComparisonId(UUID.fromString(arg))),
  pattern = root / "comparison" / segment[String] / endOfSegments
)

val loginRoute = Route.static(Page.Main, root)

object router
    extends Router[Page](
      routes = List(bindingPageRoute, loginRoute),
      getPageTitle = _ match
        case Page.Main               => "Mimalyzer"
        case Page.ComparisonPage(id) => "Mimalyzer",
      serializePage = page =>
        page match
          case Page.Main               => ""
          case Page.ComparisonPage(id) => s"comparison: ${id}"
      ,
      deserializePage = pageStr =>
        pageStr match
          case ""                 => Page.Main
          case s"comparison: $id" =>
            Page.ComparisonPage(ComparisonId(UUID.fromString(id)))
      ,
      popStateEvents = windowEvents(
        _.onPopState
      ),
      owner = unsafeWindowOwner
    )

def redirectTo(pg: Page)(using router: Router[Page]) =
  router.pushState(pg)

def forceRedirectTo(pg: Page)(using router: Router[Page]) =
  router.replaceState(pg)

def navigateTo(page: Page)(using router: Router[Page]): Binder[HtmlElement] =
  Binder { el =>
    import org.scalajs.dom

    val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

    if isLinkElement then el.amend(href(router.absoluteUrlForPage(page)))

    (onClick
      .filter(ev =>
        !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey))
      )
      .preventDefault
      --> (_ => redirectTo(page))).bind(el)
  }
