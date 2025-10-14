package mimalyzer

import cats.effect.*
import cats.syntax.all.*
import mimalyzer.protocol.*

import scala.concurrent.ExecutionContext

import std.*

// TODO: fix this
type WorkerId = java.util.UUID
type JobId = java.util.UUID

class Worker(
    id: WorkerId,
    store: Store,
    config: WorkerConfig,
    compilers: Compilers,
    singleThread: ExecutionContext,
    progress: (JobId, ProcessingStep) => IO[Unit]
):
  def process: Resource[IO, IO[OutcomeIO[Unit]]] =
    Log.info(s"Worker $id is starting").toResource *>
      Queue.bounded[IO, JobId](config.workerQueueSize).toResource.flatMap { q =>
        val normalProcess =
          fs2.Stream
            .repeatEval(q.tryTake)
            .meteredStartImmediately(config.workerPulse)
            .flatMap {
              case None =>
                val unprocessed = store
                  .createLeases(id, config.leaseLimit)
                  .evalTap(jobId =>
                    Log.info(s"Worker $id is leasing job $jobId")
                  )

                val stolen = fs2.Stream
                  .evalSeq(
                    store
                      .workSteal(id, config.workStealLimit, config.jobStaleness)
                      .flatTap(jobIds =>
                        Log
                          .info(s"Worker $id is stealing jobs $jobIds")
                          .whenA(jobIds.nonEmpty)
                      )
                  )
                  .onError(err =>
                    fs2.Stream.eval(Log.error("Failed to steal a job", err))
                  )

                (unprocessed ++ stolen).attempt
                  .collect { case Right(jid) => jid }
                  .evalMap(q.offer)

              case Some(jobId) =>
                fs2.Stream.eval {
                  Log.info(s"Processing $jobId") *>
                    handle(jobId, progress).handleErrorWith(exc =>
                      Log.error(s"Failed during handling of job $jobId", exc)
                    ) *>
                    store
                      .removeLease(id, jobId)
                      .handleErrorWith(exc =>
                        Log.error(
                          s"Failed to remove lease for job $jobId, held by worker $id",
                          exc
                        )
                      )
                }

            }

        normalProcess.compile.drain.background
      }

  def handle(jobId: JobId, progress: (JobId, ProcessingStep) => IO[Unit]) =
    store.getNoncompleteSpec(jobId).flatMap {
      case None =>
        Log.error(
          s"Job $jobId was scheduled but is not found in the database in incomplete state"
        )
      case Some(spec) =>
        import ScalaVersion.*
        progress(jobId, ProcessingStep.PICKED_UP) *>
          analyseFileCode(
            spec.codeBefore,
            spec.codeAfter,
            spec.scalaVersion match
              case SCALA_213   => compilers.scala213
              case SCALA_212   => compilers.scala212
              case SCALA_3_LTS => compilers.scala3,
            singleThread,
            spec.scalaVersion,
            processingStep => progress(jobId, processingStep)
          ).flatMap(store.complete(jobId, _))
    }
end Worker
