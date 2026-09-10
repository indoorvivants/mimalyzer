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

object Mimalyzer extends IOApp:

  override def run(args: List[String]) =
    val cli = CommandApplication.parseOrExit[CLI](args, sys.env)

    cli match
      case cli: CLI.Server =>
        val server =
          for
            store <- Store.open()
            compilers <- bootstrapCompilers
            workers <- setupWorker(store, compilers).parReplicateA(cli.workers)
            routes <- routesResource(
              TestServiceImpl(store, compilers)
            )
            server <- EmberServerBuilder
              .default[IO]
              .withPort(cli.bind.port)
              .withHost(cli.bind.host)
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
        import org.http4s.dsl.io.*

        val process =
          for
            store <- Store.open()
            compilers <- bootstrapCompilers
            worker <- setupWorker(store, compilers)
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
              .withPort(cli.bind.port)
              .withHost(cli.bind.host)
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

def bootstrapCompilers =
  for
    compilersInfo <- IO
      .fromEither(
        CompilersInfo.readFromResources.leftMap((addr, msg) =>
          RuntimeException(
            s"Failed to read compiler information at ${addr} with: [$msg]"
          )
        )
      )
      .toResource
    compilers <- IO(Compilers.load(compilersInfo)).toResource

  yield compilers

def setupWorker(store: Store, compilers: Compilers): Resource[IO, Worker] =
  for
    env <- IO.envForIO.entries.map(_.toMap).toResource
    singleThreadEC <- Resource
      .make(IO(Executors.newSingleThreadExecutor))(es => IO(es.shutdown()))
      .map(ExecutionContext.fromExecutorService)

    workerConfig <- WorkerConfig.fromEnv.toResource
    id = UUID.randomUUID()
    worker = Worker(
      id,
      store,
      workerConfig,
      compilers,
      singleThreadEC,
      store.setProcessingStep(id, _, _)
    )
  yield worker
end setupWorker
