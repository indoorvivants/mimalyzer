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

object Test extends IOApp.Simple:
  val files = Files[IO]
  val proc = Processes[IO]

  override def run: IO[Unit] =
    val scala1 =
      """
      class X {val test: Int = ???}
      """

    val scala2 =
      """
      class X {def test: Int = ???}
      """

    // files.createTempDirectory.both(files.createTem)
    for
      tmpdir1 <- files.createTempDirectory
      tmpdir2 <- files.createTempDirectory
      _ <- fs2
        .Stream(scala1)
        .through(files.writeUtf8(tmpdir1.resolve("test.scala")))
        .compile
        .drain

      _ <- fs2
        .Stream(scala2)
        .through(files.writeUtf8(tmpdir2.resolve("test.scala")))
        .compile
        .drain

      _ <- IO.println(tmpdir1) *> IO.println(tmpdir2)

      proc1 <- proc
        .spawn(
          ProcessBuilder("scala-cli", "compile", "test.scala", "-p")
            .withWorkingDirectory(tmpdir1)
        )
        .use(_.stdout.through(fs2.text.utf8Decode).compile.string)

      proc2 <- proc
        .spawn(
          ProcessBuilder("scala-cli", "compile", "test.scala", "-p")
            .withWorkingDirectory(tmpdir2)
        )
        .use(_.stdout.through(fs2.text.utf8Decode).compile.string)

      _ = println(proc1.split(File.pathSeparatorChar).toList)
      _ = println(proc2.split(File.pathSeparatorChar).toList)
    yield ()
    end for
  end run
end Test

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
