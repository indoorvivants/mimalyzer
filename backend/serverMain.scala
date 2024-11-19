package mimalyzer
package backend

import com.comcast.ip4s.*
import cats.effect.*, std.*
import org.http4s.ember.server.EmberServerBuilder
import fullstack_scala.protocol.ComparisonId
import concurrent.duration.*
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Server extends IOApp:

  override def run(args: List[String]) =
    val port = args.headOption
      .flatMap(_.toIntOption)
      .flatMap(Port.fromInt)
      .getOrElse(sys.error("port missing or invalid"))

    val server =
      for
        ref <- IO.ref(Map.empty[ComparisonId, State]).toResource
        env <- IO.envForIO.entries.map(_.toMap).toResource
        scala213 <- IO(Scala213Compiler.load(env)).toResource
        scala212 <- IO(Scala212Compiler.load(env)).toResource
        scala3 <- IO(Scala3Compiler.load(env)).toResource
        singleThreadEC <- Resource
          .make(IO(Executors.newSingleThreadExecutor))(es => IO(es.shutdown()))
          .map(ExecutionContext.fromExecutorService)

        mutex <- Mutex[IO].toResource
        compilers = Compilers(scala213, scala212, scala3)
        routes <- routesResource(
          TestServiceImpl(ref, mutex, compilers, singleThreadEC)
        )
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
end Server
