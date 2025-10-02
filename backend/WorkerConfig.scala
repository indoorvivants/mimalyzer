package mimalyzer

import cats.effect.*, std.Env
import cats.syntax.all.*
import scala.concurrent.duration.*

case class WorkerConfig(
    workStealLimit: Int,
    leaseLimit: Int,
    workerQueueSize: Int,
    workerPulse: FiniteDuration,
    jobStaleness: FiniteDuration
)

object WorkerConfig:
  def fromEnv: IO[WorkerConfig] =
    val env = Env[IO]

    def intOption(name: String, default: Int) =
      env.get(name).map(_.flatMap(_.toIntOption).getOrElse(default))

    def durationMSOption(name: String, default: FiniteDuration) =
      env
        .get(name)
        .map(_.flatMap(_.toIntOption).map(_.millis).getOrElse(default))

    val workStealLimit =
      intOption("MIMALYZER_WORKER_WORK_STEAL_LIMIT", 5)

    val leaseLimit =
      intOption("MIMALYZER_WORKER_LEASE_LIMIT", 5)

    val workerQueueSize =
      intOption("MIMALYZER_WORKER_QUEUE_SIZE", 100)

    val workerPulseDuration =
      durationMSOption("MIMALYZER_WORKER_PULSE_DURATION_MS", 1.second)

    val jobStalenessDuration =
      durationMSOption("MIMALYZER_WORKER_STALE_DURATION_MS", 60.seconds)

    (
      workStealLimit,
      leaseLimit,
      workerQueueSize,
      workerPulseDuration,
      jobStalenessDuration
    ).parMapN(
      WorkerConfig.apply
    )
  end fromEnv
end WorkerConfig
