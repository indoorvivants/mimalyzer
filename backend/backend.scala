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
import cats.effect.std.Mutex

def handleErrors(logger: Scribe[IO], routes: HttpApp[IO]): HttpApp[IO] =
  import cats.syntax.all.*
  routes.onError { exc =>
    Kleisli(request => logger.error("Request failed", request.toString, exc))
  }

enum Status:
  case Completed(problems: List[Problem])
  case InProgress(msg: String)

case class State(comparison: Comparison, status: Status)

class TestServiceImpl(ref: Ref[IO, Map[ComparisonId, State]], mutex: Mutex[IO])
    extends MimaService[IO]:
  val randomID = UUIDGen[IO].randomUUID.map(ComparisonId(_))

  private def checkCode(code: ScalaCode, label: CodeLabel) =
    val MAX_SIZE = 2048
    val len = code.value.getBytes().length
    IO.raiseWhen(len > MAX_SIZE)(CodeTooBig(len, MAX_SIZE, label))

  private def checkScalaVersion(vers: ScalaVersion) =
    val twelve = raw"2.12.(\d{1,2})".r
    val thirteen = raw"2.13.(\d{1,2})".r
    val three = raw"3.(\d{1,2}).(\d{1,2})".r
    val rgx = s"$twelve|$thirteen|$three".r

    IO.raiseUnless(rgx.matches(vers.value))(InvalidScalaVersion())

  override def createComparison(attributes: ComparisonAttributes) =
    checkScalaVersion(attributes.scalaVersion) *>
    checkCode(attributes.beforeScalaCode, CodeLabel.BEFORE) *>
      checkCode(attributes.afterScalaCode, CodeLabel.AFTER) *>
      randomID.flatMap: id =>
        mutex.lock
          .surround(
            analyseFileCode(
              attributes.beforeScalaCode,
              attributes.afterScalaCode,
              attributes.scalaVersion
            )
          )
          .flatMap: problems =>
            // ref.update(
            //   _.updated(
            //     id,
            //     State(Comparison(id, attributes), Status.Completed(problems))
            //   )
            // ) *>
            IO.pure(CreateComparisonOutput(id, problems))
  override def getComparison(id: ComparisonId) = ???
end TestServiceImpl

def routesResource(service: MimaService[IO]) =
  import org.http4s.implicits.*
  SimpleRestJsonBuilder
    .routes(service)
    .resource
    .map(_.orNotFound)
