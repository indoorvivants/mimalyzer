package mimalyzer
package backend

import cats.data.Kleisli
import cats.effect.std.{Mutex, UUIDGen}
import cats.effect.{IO, Ref}
import fullstack_scala.protocol.*
import fullstack_scala.protocol.ScalaVersion.{SCALA_212, SCALA_213, SCALA_3_LTS}
import org.http4s.HttpApp
import scribe.Scribe
import smithy4s.http4s.SimpleRestJsonBuilder

import scala.concurrent.ExecutionContext

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
    ref: Ref[IO, Map[ComparisonId, State]],
    mutex: Mutex[IO],
    compilers: Compilers,
    singleThread: ExecutionContext
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
      randomID.flatMap: id =>
        mutex.lock
          .surround(
            analyseFileCode(
              attributes.beforeScalaCode,
              attributes.afterScalaCode,
              attributes.scalaVersion match
                case SCALA_213   => compilers.scala213
                case SCALA_212   => compilers.scala212
                case SCALA_3_LTS => compilers.scala3,
              singleThread,
              attributes.scalaVersion
            )
          )
          .map: summary =>
            CreateComparisonOutput(
              id, 
              Option.when(summary.mima.nonEmpty)(MimaProblems(summary.mima)),
              Option.when(summary.tasty.nonEmpty)(TastyMimaProblems(summary.tasty))
            )
  override def getComparison(id: ComparisonId) = ???
end TestServiceImpl

def routesResource(service: MimaService[IO]) =
  import org.http4s.implicits.*
  SimpleRestJsonBuilder
    .routes(service)
    .resource
    .map(_.orNotFound)
