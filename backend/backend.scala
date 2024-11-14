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
import cats.effect.std.UUIDGen
import concurrent.duration.*

def handleErrors(logger: Scribe[IO], routes: HttpApp[IO]): HttpApp[IO] =
  import cats.syntax.all.*
  routes.onError { exc =>
    Kleisli(request => logger.error("Request failed", request.toString, exc))
  }

enum Status:
  case Completed(problems: List[Problem])
  case InProgress(msg: String)

case class State(comparison: Comparison, status: Status)

class TestServiceImpl(ref: Ref[IO, Map[ComparisonId, State]])
    extends MimaService[IO]:
  val randomID = UUIDGen[IO].randomUUID.map(ComparisonId(_))
  override def createComparison(attributes: ComparisonAttributes) =
    randomID.flatMap: id =>
      analyseFileCode(
        attributes.beforeScalaCode,
        attributes.afterScalaCode,
        attributes.scalaVersion
      ).flatMap: problems =>
        ref.update(
          _.updated(
            id,
            State(Comparison(id, attributes), Status.Completed(problems))
          )
        ) *>
          IO.pure(CreateComparisonOutput(id, problems))
  override def getComparison(id: ComparisonId) = ???
end TestServiceImpl

def routesResource(service: MimaService[IO]) =
  import org.http4s.implicits.*
  SimpleRestJsonBuilder
    .routes(service)
    .resource
    .map(_.orNotFound)
