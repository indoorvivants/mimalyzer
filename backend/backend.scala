package mimalyzer
package backend

import cats.data.Kleisli
import cats.effect.IO
import cats.effect.std.UUIDGen
import mimalyzer.protocol.*
import org.http4s.HttpApp
import scribe.Scribe
import smithy4s.http4s.SimpleRestJsonBuilder

def handleErrors(logger: Scribe[IO], routes: HttpApp[IO]): HttpApp[IO] =
  import cats.syntax.all.*
  routes.onError { exc =>
    Kleisli(request => logger.error("Request failed", request.toString, exc))
  }

enum Status:
  case Completed(problems: List[Problem])
  case InProgress(msg: String)

case class State(comparison: Comparison, status: Status)

class TestServiceImpl(
    store: Store
) extends MimaService[IO]:
  val randomID = UUIDGen[IO].randomUUID.map(ComparisonId(_))

  override def health() = IO.pure(HealthOutput(status = "ok"))

  private def checkCode(code: ScalaCode, label: CodeLabel) =
    val MAX_SIZE = 2048
    val len = code.value.getBytes().length
    IO.raiseWhen(len > MAX_SIZE)(CodeTooBig(len, MAX_SIZE, label))

  override def createComparison(attributes: ComparisonAttributes) =
    checkCode(attributes.beforeScalaCode, CodeLabel.BEFORE) *>
      checkCode(attributes.afterScalaCode, CodeLabel.AFTER) *>
      store
        .schedule(attributes)
        .map: id =>
          CreateComparisonOutput(id)

  override def getComparison(id: ComparisonId) =
    store.getComparison(id).map(GetComparisonOutput(_))

  override def getStatus(id: ComparisonId): IO[GetStatusOutput] =
    store.getStatus(id).map(st => GetStatusOutput(st))
end TestServiceImpl

def routesResource(service: MimaService[IO]) =
  import org.http4s.implicits.*
  SimpleRestJsonBuilder
    .routes(service)
    .resource
    .map(_.orNotFound)
