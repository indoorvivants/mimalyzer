package fullstack_scala
package backend

import cats.data.Kleisli
import cats.effect.IO
import fullstack_scala.protocol.*
import org.http4s.HttpApp
import scribe.Scribe
import smithy4s.http4s.SimpleRestJsonBuilder
import cats.effect.std.Random
import cats.effect.Ref

def handleErrors(logger: Scribe[IO], routes: HttpApp[IO]): HttpApp[IO] =
  import cats.syntax.all.*
  routes.onError { exc =>
    Kleisli(request => logger.error("Request failed", request.toString, exc))
  }

class TestServiceImpl(ref: Ref[IO, List[Test]]) extends TestService[IO]:
  override def listTests(): IO[ListTestsOutput] =
    ref.get.map(ListTestsOutput(_))

  override def createTest(attributes: TestAttributes) =
    Random
      .scalaUtilRandom[IO]
      .flatMap(_.nextInt)
      .map(id => Test(id = TestId(id), attributes = attributes))
      .flatTap(test => ref.update(_ :+ test))
      .map(CreateTestOutput(_))

end TestServiceImpl

def routesResource(service: TestService[IO]) =
  import org.http4s.implicits.*
  SimpleRestJsonBuilder
    .routes(service)
    .resource
    .map(_.orNotFound)
