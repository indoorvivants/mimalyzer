package mimalyzer
package backend

import com.comcast.ip4s.*
import cats.effect.*, std.*
import org.http4s.ember.server.EmberServerBuilder
import fullstack_scala.protocol.ComparisonId
import concurrent.duration.*


object Server extends IOApp:

  override def run(args: List[String]) =
    val port = args.headOption
      .flatMap(_.toIntOption)
      .flatMap(Port.fromInt)
      .getOrElse(sys.error("port missing or invalid"))

    val server =
      for
        ref <- IO.ref(Map.empty[ComparisonId, State]).toResource
        mutex <- Mutex[IO].toResource
        routes <- routesResource(TestServiceImpl(ref, mutex))
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
