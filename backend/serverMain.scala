package mimalyzer
package backend

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import decline_derive.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpApp, MediaType}

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

import concurrent.duration.*

enum CLI derives CommandApplication:
  case Server(port: Option[Int], host: Option[String], workers: Option[Int])
  case Worker(port: Option[Int], host: Option[String])

object Mimalyzer extends IOApp:

  override def run(args: List[String]) =
    val cli = CommandApplication.parseOrExit[CLI](args, sys.env)

    cli match
      case cli: CLI.Server =>
        val (port, host) = defaults(cli.port, cli.host)
        val workers = cli.workers.getOrElse(1)

        val server =
          for
            store <- Store.open()
            workers <- setupWorker(store).parReplicateA(workers)
            routes <- routesResource(
              TestServiceImpl(store)
            )
            server <- EmberServerBuilder
              .default[IO]
              .withPort(port)
              .withHost(host)
              .withHttpApp(handleErrors(scribe.cats.io, routes))
              .withShutdownTimeout(0.seconds)
              .build
              .map(_.baseUri)
              .evalTap(uri => Log.info(s"Server running on $uri"))
              .parProductL(workers.parTraverse(_.process))
          yield server

        server.useForever
          .as(ExitCode.Success)

      case cli: CLI.Worker =>
        val (port, host) = defaults(cli.port, cli.host)

        import org.http4s.dsl.io.*

        val process =
          for
            store <- Store.open()
            worker <- setupWorker(store)
            routes = HttpApp[IO]:
              case GET -> Root / "health" =>
                Ok("""{"status": "ok"}""").map(
                  _.withContentType(
                    org.http4s.headers.`Content-Type`(
                      MediaType.application.json
                    )
                  )
                )

            server <- EmberServerBuilder
              .default[IO]
              .withPort(port)
              .withHost(host)
              .withHttpApp(handleErrors(scribe.cats.io, routes))
              .withShutdownTimeout(0.seconds)
              .build
              .map(_.baseUri)
              .evalTap(uri => Log.info(s"Worker health running on $uri"))
              .parProductL(worker.process)
          yield server
        end process

        process.useForever.as(ExitCode.Success)
    end match

  end run
end Mimalyzer

def defaults(portOpt: Option[Int], hostOpt: Option[String]) =
  val port = portOpt
    .flatMap(port =>
      Port.fromInt(port).orElse(sys.error(s"Invalid port $port"))
    )
    .getOrElse(port"8080")

  val host = hostOpt
    .flatMap(host =>
      Host.fromString(host).orElse(sys.error(s"Invalid host $host"))
    )
    .getOrElse(host"localhost")

  port -> host
end defaults

def setupWorker(store: Store) =
  for
    env <- IO.envForIO.entries.map(_.toMap).toResource
    scala213 <- IO(Scala213Compiler.load(env)).toResource
    scala212 <- IO(Scala212Compiler.load(env)).toResource
    scala3 <- IO(Scala3Compiler.load(env)).toResource
    singleThreadEC <- Resource
      .make(IO(Executors.newSingleThreadExecutor))(es => IO(es.shutdown()))
      .map(ExecutionContext.fromExecutorService)

    compilers = Compilers(scala213, scala212, scala3)
    workerConfig <- WorkerConfig.fromEnv.toResource
    worker = Worker(
      UUID.randomUUID(),
      store,
      workerConfig,
      compilers,
      singleThreadEC,
      store.setProcessingStep
    )
  yield worker
