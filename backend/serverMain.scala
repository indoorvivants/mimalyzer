package fullstack_scala
package backend

import cats.effect.*
import com.comcast.ip4s.Port
import com.comcast.ip4s.host
import org.http4s.ember.server.EmberServerBuilder

import scala.concurrent.duration.*
import fullstack_scala.protocol.*
import fs2.io.file.Files
import fs2.io.process.Processes, fs2.io.process.ProcessBuilder
import java.io.File

import com.typesafe.tools.mima.lib.MiMaLib

val files = Files[IO]
val proc = Processes[IO]

def analyseFileCode(
    oldScala: ScalaCode,
    newScala: ScalaCode,
    scalaVersion: ScalaVersion
) =
  for
    tmpdir1 <- files.createTempDirectory
    tmpdir2 <- files.createTempDirectory
    _ <- fs2
      .Stream(oldScala.value)
      .through(files.writeUtf8(tmpdir1.resolve("old.scala")))
      .compile
      .drain

    _ <- fs2
      .Stream(newScala.value)
      .through(files.writeUtf8(tmpdir2.resolve("new.scala")))
      .compile
      .drain

    proc1 <- proc
      .spawn(
        ProcessBuilder(
          "scala-cli",
          "compile",
          "old.scala",
          "-p",
          "-S",
          scalaVersion.value
        )
          .withWorkingDirectory(tmpdir1)
      )
      .use(_.stdout.through(fs2.text.utf8Decode).compile.string)

    proc2 <- proc
      .spawn(
        ProcessBuilder(
          "scala-cli",
          "compile",
          "new.scala",
          "-p",
          "-S",
          scalaVersion.value
        )
          .withWorkingDirectory(tmpdir2)
      )
      .use(_.stdout.through(fs2.text.utf8Decode).compile.string)

    classes1 :: classpath1 = proc1.split(File.pathSeparatorChar).toList
    classes2 :: classpath2 = proc2.split(File.pathSeparatorChar).toList

    lib = new MiMaLib(classpath1.map(new File(_)))

    problems <- IO.blocking(
      lib.collectProblems(new File(classes1), new File(classes2), Nil)
    )

    _ <- files.deleteRecursively(tmpdir1)
    _ <- files.deleteRecursively(tmpdir2)
  yield problems.map(p => Problem(Some(p.toString)))
  end for
end analyseFileCode

object Server extends IOApp:

  override def run(args: List[String]) =
    val port = args.headOption
      .flatMap(_.toIntOption)
      .flatMap(Port.fromInt)
      .getOrElse(sys.error("port missing or invalid"))

    val server =
      for
        ref <- IO.ref(DUMMY_DATA).toResource
        routes <- routesResource(TestServiceImpl(ref))
        server <- EmberServerBuilder
          .default[IO]
          .withPort(port)
          .withHost(host"0.0.0.0")
          .withHttpApp(handleErrors(scribe.cats.io, routes))
          .withShutdownTimeout(0.seconds)
          .build
          .map(_.baseUri)
          .evalTap(uri => IO.println(s"Server running on $uri"))
      yield server

    server.useForever
      .as(ExitCode.Success)

  end run

  val DUMMY_DATA =
    Map.empty[ComparisonId, State]

end Server
