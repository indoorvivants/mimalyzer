package fullstack_scala
package frontend

import fullstack_scala.protocol.*
import smithy4s_fetch.SimpleRestJsonFetchClient
import com.raquo.laminar.api.L.*
import scala.scalajs.js.Promise
import org.scalajs.dom

def createApiClient(uri: String) = SimpleRestJsonFetchClient(
  ComparisonService,
  uri
).make

extension [T](p: => Promise[T]) inline def stream = EventStream.fromJsPromise(p)

enum Action:
  case GenerateNewTest

// def renderTest(id: ComparisonId, init: , signal: Signal[Test]) =
//   div(
//     cls := "flex flex-col gap-2 border-1 border-slate-100 p-4 rounded-xl",
//     p(
//       i("ID=", id.toString()),
//       p(
//         b("Title: "),
//         child.text <-- signal.map(_.attributes.title.value)
//       ),
//       p(
//         b("Description: "),
//         blockQuote(
//           child.maybe <-- signal.map(
//             _.attributes.description.map(_.value)
//           )
//         )
//       )
//     )
//   )

@main def hello =
  // State
  val allTests = Var(List.empty[Test])
  val actionBus = EventBus[Action]()

  val apiClient = createApiClient(dom.window.location.href)

  val loadInitialList =
    apiClient.listTests().stream.map(_.tests) --> allTests

  val renderList =
    children <-- allTests.signal.split(_.id)(renderTest)

  val handleEvents =
    actionBus.events --> { case Action.GenerateNewTest =>
      apiClient
        .createTest(
          attributes = TestAttributes(
            title = TestTitle("hello"),
            description = Some(TestDescription("wut"))
          )
        )
        .`then`(test => allTests.update(_ :+ test.test))
    }

  val btn =
    button(
      "Generate new test",
      onClick.mapToStrict(Action.GenerateNewTest) --> actionBus,
      cls := "bg-sky-700 text-lg font-bold p-2 text-white"
    )

  val app =
    div(
      cls := "content mx-auto w-6/12 rounded-lg border-2 border-slate-400 p-4",
      p("Welcome to Scala full stack template!", cls := "text-2xl m-2"),
      div(
        cls := "flex gap-4",
        img(
          src := "https://www.scala-lang.org/resources/img/frontpage/scala-spiral.png",
          cls := "w-24"
        ),
        div(
          cls := "w-full",
          loadInitialList,
          btn,
          renderList,
          handleEvents
        )
      )
    )

  renderOnDomContentLoaded(dom.document.getElementById("content"), app)
end hello
