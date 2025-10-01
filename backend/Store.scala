package mimalyzer

import skunk.*, implicits.*
import cats.effect.*, std.*
import cats.syntax.all.*
import org.typelevel.otel4s.trace.Tracer
import dumbo.Dumbo
import dumbo.ConnectionConfig

import fullstack_scala.protocol.*
import java.util.UUID
import scala.concurrent.duration.FiniteDuration
import java.time.OffsetDateTime
import java.time.ZoneId
import skunk.codec.all.*
import smithy4s.Newtype
import fullstack_scala.protocol.CodeLabel.AFTER
import fullstack_scala.protocol.CodeLabel.BEFORE

enum State:
  case Added
  case Completed
  case Failed
  case Processing

  def stringValue =
    this match
      case Added      => "added"
      case Completed  => "completed"
      case Failed     => "failed"
      case Processing => "processing"

end State

case class CompilationJob(
    comparisonId: ComparisonId,
    jobId: JobId,
    codeBefore: ScalaCode,
    codeAfter: ScalaCode,
    scalaVersion: ScalaVersion
)

object State:
  val mapping = State.values.map(v => v.stringValue -> v).toMap
  def fromString(s: String) = mapping.get(s)

class Store private (db: Resource[IO, Session[IO]]):
  def schedule(attributes: ComparisonAttributes): IO[ComparisonId] =
    IO.realTimeInstant.flatMap { instant =>
      UUIDGen.randomUUID[IO].map(ComparisonId(_)).flatMap { id =>
        db.use(s =>
          s.transaction.use: _ =>
            val insertJob = s.execute(
              sql"""
                  |insert into jobs (id, code_before, code_after, created_at)
                  | values (${C.comparisonId},  ${C.scalaCode}, ${C.scalaCode}, $timestamptz);""".stripMargin.command
            )(
              id,
              attributes.beforeScalaCode,
              attributes.afterScalaCode,
              OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"))
            )

            val insertCompilationResult =
              s.execute(
                sql"""
                    |insert into compilation_results (job_id, scala_version_tag, state, created_at)
                    | values (${C.comparisonId},  ${C.scalaVersion}, ${C.state}, $timestamptz);""".stripMargin.command
              )(
                id,
                attributes.scalaVersion,
                State.Added,
                OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"))
              )
            end insertCompilationResult

            insertJob *> insertCompilationResult
        ).as(id)
      }
    }

  def getComparison(id: ComparisonId) =
    db.use(
      _.unique(
        sql"""
        select j.id, j.code_before, j.code_after, cr.scala_version_tag, cr.mima_problems, cr.tasty_mima_problems
        from jobs j
        inner join compilation_results cr on cr.job_id = j.id
        where j.id = ${C.comparisonId} and cr.state = 'completed'
        """.query(
          (
            C.comparisonId,
            C.comparisonAttributes,
            C.mimaProblems.opt,
            C.tastyMimaProblems.opt
          ).tupled
        )
      )(id)
    ).map(Comparison.apply)

  def getStatus(id: ComparisonId): IO[ComparisonStatus] =
    // TODO: when going to multi-scala requests, make this a stream
    db.use(
      _.option(sql"""
      select cr.state, cr.processing_step, before_compilation_errors, after_compilation_errors
      from jobs j
      inner join compilation_results cr on cr.job_id = j.id
      where j.id = ${C.comparisonId}
      """.query((C.state, C.processingStep.opt, text.opt, text.opt).tupled))(id)
    ).map: o =>
      o.map:
        case (state, processingStep, beforeErrors, afterErrors) =>
          state match
            case State.Added     => ComparisonStatus.waiting(Waiting())
            case State.Completed => ComparisonStatus.completed(Completed())
            case State.Failed    =>
              val compFailed =
                beforeErrors
                  .map(CompilationFailed(CodeLabel.BEFORE, _))
                  .orElse(
                    afterErrors.map(CompilationFailed(CodeLabel.AFTER, _))
                  )

              ComparisonStatus.failed(
                compFailed.getOrElse(
                  CompilationFailed(CodeLabel.BEFORE, "Internal server error")
                )
              )
            case State.Processing =>
              ComparisonStatus.processing(
                Processing(step = processingStep)
              )
      .getOrElse(ComparisonStatus.notFound(NotFound()))

  def createLeases(workerId: UUID, limit: Int): fs2.Stream[IO, UUID] =
    fs2.Stream
      .eval(
        IO.realTimeInstant.map(OffsetDateTime.ofInstant(_, ZoneId.of("UTC")))
      )
      .flatMap { inst =>
        fs2.Stream.evalSeq(
          db.use(
            _.stream(
              sql"""
                update compilation_results
                set
                    worker_id = $uuid,
                    worker_checked_in_at = ${timestamptz}
                where id in
                    (
                        select
                            id
                        from compilation_results
                        where state = ${C.state} and worker_id is null
                        order by created_at
                        limit $int4
                    )
                returning id
              """.stripMargin.query(uuid),
              (workerId, inst, State.Added, limit),
              limit.min(100)
            ).compile.toList
          )
        )
      }

  end createLeases

  val instant =
    IO.realTimeInstant
      .map(OffsetDateTime.ofInstant(_, ZoneId.of("UTC")))

  def workSteal(
      workerId: UUID,
      limit: Int,
      staleness: FiniteDuration
  ): IO[List[UUID]] =
    db.use(
      _.stream(
        sql"""
          update compilation_results
          set worker_id = ${uuid}, worker_checked_in_at = now(), state = ${C.state}
          where id
            in (select id from compilation_results where (now() - worker_checked_in_at > interval '$int4 seconds') and state NOT IN  (${C.state
            .list(2)}) limit $int4)
          returning id
          """.query(uuid),
        (
          workerId,
          State.Processing,
          staleness.toSeconds.toInt,
          List(State.Completed, State.Failed),
          limit
        ),
        limit
      ).compile.toList
    )

  def removeLease(id: WorkerId, job: JobId): IO[Unit] =
    db.use(
      _.execute(
        sql"""
        update compilation_results
        set worker_id = null, worker_checked_in_at = null
        where id = ${uuid} and worker_id = ${uuid}
    """.command,
        (id, job)
      )
    ).void

  def getNoncompleteSpec(id: JobId): IO[Option[CompilationJob]] =
    db.use(
      _.option(
        sql"""
        select job_id, cr.id, j.code_before, j.code_after, cr.scala_version_tag
        from compilation_results cr
        inner join jobs j on j.id = cr.job_id
        where cr.id = ${uuid} and cr.state != ${C.state} and cr.state != ${C.state}
        """.query(C.compilationJob),
        (id, State.Completed, State.Failed)
      )
    )

  def setProcessingStep(id: JobId, step: ProcessingStep): IO[Unit] =
    db.use(
      _.execute(
        sql"""update compilation_results set
            state=${C.state},
            processing_step = ${C.processingStep},
            worker_checked_in_at = now()
            where id = ${C.jobId}""".command
      )(State.Processing, step, id)
    ).void

  def complete(id: JobId, result: ComparisonResult): IO[Unit] =
    result match
      case ComparisonResult.CompilationFailed(which, errorOut) =>
        val errorField = which match
          case AFTER  => sql"after_compilation_errors"
          case BEFORE => sql"before_compilation_errors"

        db.use(
          _.execute(sql"""
          update compilation_results
          set state = ${C.state}, $errorField = $text
          where id = ${uuid}
          """.command)(
            (State.Failed, errorOut, id)
          )
        ).void
      case ComparisonResult.Success(mimaProblems, tastyMimaProblems) =>
        db.use(
          _.execute(sql"""
        update compilation_results
            set mima_problems = ${C.mimaProblems.opt},
                tasty_mima_problems = ${C.tastyMimaProblems.opt},
                state = ${C.state},
                worker_checked_in_at = now()
            where id = ${uuid}
    """.command)(
            Option.when(mimaProblems.problems.nonEmpty)(
              mimaProblems
            ),
            Option.when(tastyMimaProblems.problems.nonEmpty)(
              tastyMimaProblems
            ),
            State.Completed,
            id
          )
        ).void

end Store

object C:
  def imap[A](nt: Newtype[A])(codec: Codec[A]): Codec[nt.T] =
    codec.imap[nt.T](nt.apply(_))(nt.value)

  def enumap[A <: smithy4s.Enumeration.Value](nt: smithy4s.Enumeration[A])(
      codec: Codec[String]
  ): Codec[A] =
    val values = nt.valueMap
    val concat = nt.values.mkString(", ")
    codec.eimap(raw =>
      values
        .get(raw)
        .toRight(s"Unknown value $raw – acceptable values are: $concat")
    )(_.value)
  end enumap

  import io.circe.parser.decode
  import io.circe.syntax.*

  def jsonLike[T: io.circe.Decoder: io.circe.Encoder](
      base: Codec[String]
  ): Codec[T] =
    base
      .imap[T](str => decode[T](str).right.get)(_.asJson.noSpacesSortKeys)

  case class JsonProblem(msg: String) derives io.circe.Codec.AsObject:
    def toProblem = Problem(msg)

  val comparisonId = imap(ComparisonId)(uuid)
  val scalaCode = imap(ScalaCode)(text)
  val scalaVersion = enumap(ScalaVersion)(varchar(10))
  val processingStep = enumap(ProcessingStep)(text)
  val jsonProblemList = jsonLike[List[JsonProblem]](text)
  val problemList =
    jsonProblemList.imap(_.map(_.toProblem))(_.map(p => JsonProblem(p.message)))
  val mimaProblems = problemList.imap(MimaProblems(_))(_.problems)
  val tastyMimaProblems = problemList.imap(TastyMimaProblems(_))(_.problems)
  val comparisonAttributes =
    (scalaCode, scalaCode, scalaVersion).tupled.asDecoder
      .map(ComparisonAttributes.apply)
  val state = varchar(100).eimap(s =>
    State
      .fromString(s)
      .toRight(
        s"Unknown value $s, acceptable values are: ${State.values.mkString(", ")}"
      )
  )(_.stringValue)

  lazy val compilationJob =
    (comparisonId, jobId, scalaCode, scalaCode, scalaVersion).tupled.asDecoder
      .map(CompilationJob.apply)

  val jobId = uuid
end C

object Store:
  def open(): Resource[cats.effect.IO, Store] =
    val creds =
      Env[IO].entries
        .flatTap(entries => Log.info(s"Env: ${entries.toMap.keySet}"))
        .map: env =>
          PgCredentials.defaults(env.toMap)

    given Tracer[IO] = Tracer.Implicits.noop[IO]

    creds
      .flatTap(cr => Log.info(s"Credentials: $cr"))
      .toResource
      .evalTap(migrate(_))
      .flatMap(
        open(_, SkunkConfig)
      )
  end open

  def migrate(postgres: PgCredentials)(using Tracer[IO]) =

    given dumbo.logging.Logger[IO] =
      case (dumbo.logging.LogLevel.Info, message) => Log.info(message)
      case (dumbo.logging.LogLevel.Warn, message) => Log.warn(message)

    Dumbo
      .withResourcesIn[IO]("db/migration")
      .apply(
        connection = ConnectionConfig(
          host = postgres.host,
          port = postgres.port,
          user = postgres.user,
          database = postgres.database,
          password = postgres.password,
          ssl =
            if postgres.ssl then ConnectionConfig.SSL.Trusted
            else ConnectionConfig.SSL.None
        ),
        defaultSchema = "public"
      )
      .runMigration
  end migrate

  def open(postgres: PgCredentials, skunkConfig: SkunkConfig)(using
      Tracer[IO]
  ): Resource[IO, Store] =
    Session
      .pooled[IO](
        host = postgres.host,
        port = postgres.port,
        user = postgres.user,
        database = postgres.database,
        password = postgres.password,
        strategy = skunkConfig.strategy,
        max = skunkConfig.maxSessions,
        debug = skunkConfig.debug,
        ssl = if postgres.ssl then skunk.SSL.Trusted else skunk.SSL.None
      )
      .map(Store(_))

end Store
