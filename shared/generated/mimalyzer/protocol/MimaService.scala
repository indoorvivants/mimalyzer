package mimalyzer.protocol

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.ErrorSchema
import smithy4s.schema.OperationSchema
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union
import smithy4s.schema.Schema.unit

trait MimaServiceGen[F[_, _, _, _, _]] {
  self =>

  /** HTTP GET /api/status/{id} */
  def getStatus(id: ComparisonId): F[GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing]
  /** HTTP PUT /api/comparison */
  def createComparison(attributes: ComparisonAttributes): F[CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing]
  /** HTTP GET /api/comparison/{id} */
  def getComparison(id: ComparisonId): F[GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing]
  /** HTTP GET /api/health */
  def health(): F[Unit, Nothing, HealthOutput, Nothing, Nothing]

  final def transform: Transformation.PartiallyApplied[MimaServiceGen[F]] = Transformation.of[MimaServiceGen[F]](this)
}

object MimaServiceGen extends Service.Mixin[MimaServiceGen, MimaServiceOperation] {

  val id: ShapeId = ShapeId("mimalyzer.protocol", "MimaService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[MimaServiceOperation, _, _, _, _, _]] = Vector(
    MimaServiceOperation.GetStatus,
    MimaServiceOperation.CreateComparison,
    MimaServiceOperation.GetComparison,
    MimaServiceOperation.Health,
  )

  def input[I, E, O, SI, SO](op: MimaServiceOperation[I, E, O, SI, SO]): I = op.input
  def ordinal[I, E, O, SI, SO](op: MimaServiceOperation[I, E, O, SI, SO]): Int = op.ordinal
  override def endpoint[I, E, O, SI, SO](op: MimaServiceOperation[I, E, O, SI, SO]) = op.endpoint
  class Constant[P[-_, +_, +_, +_, +_]](value: P[Any, Nothing, Nothing, Nothing, Nothing]) extends MimaServiceOperation.Transformed[MimaServiceOperation, P](reified, const5(value))
  type Default[F[+_]] = Constant[smithy4s.kinds.stubs.Kind1[F]#toKind5]
  def reified: MimaServiceGen[MimaServiceOperation] = MimaServiceOperation.reified
  def mapK5[P[_, _, _, _, _], P1[_, _, _, _, _]](alg: MimaServiceGen[P], f: PolyFunction5[P, P1]): MimaServiceGen[P1] = new MimaServiceOperation.Transformed(alg, f)
  def fromPolyFunction[P[_, _, _, _, _]](f: PolyFunction5[MimaServiceOperation, P]): MimaServiceGen[P] = new MimaServiceOperation.Transformed(reified, f)
  def toPolyFunction[P[_, _, _, _, _]](impl: MimaServiceGen[P]): PolyFunction5[MimaServiceOperation, P] = MimaServiceOperation.toPolyFunction(impl)

  type CreateComparisonError = MimaServiceOperation.CreateComparisonError
  val CreateComparisonError = MimaServiceOperation.CreateComparisonError
  type GetComparisonError = MimaServiceOperation.GetComparisonError
  val GetComparisonError = MimaServiceOperation.GetComparisonError
}

sealed trait MimaServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[MimaServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object MimaServiceOperation {

  object reified extends MimaServiceGen[MimaServiceOperation] {
    def getStatus(id: ComparisonId): GetStatus = GetStatus(GetStatusInput(id))
    def createComparison(attributes: ComparisonAttributes): CreateComparison = CreateComparison(CreateComparisonInput(attributes))
    def getComparison(id: ComparisonId): GetComparison = GetComparison(GetComparisonInput(id))
    def health(): Health = Health()
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: MimaServiceGen[P], f: PolyFunction5[P, P1]) extends MimaServiceGen[P1] {
    def getStatus(id: ComparisonId): P1[GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing] = f[GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing](alg.getStatus(id))
    def createComparison(attributes: ComparisonAttributes): P1[CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing] = f[CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing](alg.createComparison(attributes))
    def getComparison(id: ComparisonId): P1[GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing] = f[GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing](alg.getComparison(id))
    def health(): P1[Unit, Nothing, HealthOutput, Nothing, Nothing] = f[Unit, Nothing, HealthOutput, Nothing, Nothing](alg.health())
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: MimaServiceGen[P]): PolyFunction5[MimaServiceOperation, P] = new PolyFunction5[MimaServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: MimaServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GetStatus(input: GetStatusInput) extends MimaServiceOperation[GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing] = impl.getStatus(input.id)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[MimaServiceOperation,GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing] = GetStatus
  }
  object GetStatus extends smithy4s.Endpoint[MimaServiceOperation,GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetStatusInput, Nothing, GetStatusOutput, Nothing, Nothing] = Schema.operation(ShapeId("mimalyzer.protocol", "GetStatus"))
      .withInput(GetStatusInput.schema)
      .withOutput(GetStatusOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/api/status/{id}"), code = 200), smithy.api.Readonly())
    def wrap(input: GetStatusInput): GetStatus = GetStatus(input)
  }
  final case class CreateComparison(input: CreateComparisonInput) extends MimaServiceOperation[CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing] = impl.createComparison(input.attributes)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[MimaServiceOperation,CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing] = CreateComparison
  }
  object CreateComparison extends smithy4s.Endpoint[MimaServiceOperation,CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing] {
    val schema: OperationSchema[CreateComparisonInput, MimaServiceOperation.CreateComparisonError, CreateComparisonOutput, Nothing, Nothing] = Schema.operation(ShapeId("mimalyzer.protocol", "CreateComparison"))
      .withInput(CreateComparisonInput.schema)
      .withError(CreateComparisonError.errorSchema)
      .withOutput(CreateComparisonOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("PUT"), uri = smithy.api.NonEmptyString("/api/comparison"), code = 200), smithy.api.Idempotent())
    def wrap(input: CreateComparisonInput): CreateComparison = CreateComparison(input)
  }
  sealed trait CreateComparisonError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: CreateComparisonError = this
    def $ordinal: Int

    object project {
      def codeTooBig: Option[CodeTooBig] = CreateComparisonError.CodeTooBigCase.alt.project.lift(self).map(_.codeTooBig)
      def invalidScalaVersion: Option[InvalidScalaVersion] = CreateComparisonError.InvalidScalaVersionCase.alt.project.lift(self).map(_.invalidScalaVersion)
    }

    def accept[A](visitor: CreateComparisonError.Visitor[A]): A = this match {
      case value: CreateComparisonError.CodeTooBigCase => visitor.codeTooBig(value.codeTooBig)
      case value: CreateComparisonError.InvalidScalaVersionCase => visitor.invalidScalaVersion(value.invalidScalaVersion)
    }
  }
  object CreateComparisonError extends ErrorSchema.Companion[CreateComparisonError] {

    def codeTooBig(codeTooBig: CodeTooBig): CreateComparisonError = CodeTooBigCase(codeTooBig)
    def invalidScalaVersion(invalidScalaVersion: InvalidScalaVersion): CreateComparisonError = InvalidScalaVersionCase(invalidScalaVersion)

    val id: ShapeId = ShapeId("mimalyzer.protocol", "CreateComparisonError")

    val hints: Hints = Hints.empty

    final case class CodeTooBigCase(codeTooBig: CodeTooBig) extends CreateComparisonError { final def $ordinal: Int = 0 }
    final case class InvalidScalaVersionCase(invalidScalaVersion: InvalidScalaVersion) extends CreateComparisonError { final def $ordinal: Int = 1 }

    object CodeTooBigCase {
      val hints: Hints = Hints.empty
      val schema: Schema[CreateComparisonError.CodeTooBigCase] = bijection(CodeTooBig.schema.addHints(hints), CreateComparisonError.CodeTooBigCase(_), _.codeTooBig)
      val alt = schema.oneOf[CreateComparisonError]("CodeTooBig")
    }
    object InvalidScalaVersionCase {
      val hints: Hints = Hints.empty
      val schema: Schema[CreateComparisonError.InvalidScalaVersionCase] = bijection(InvalidScalaVersion.schema.addHints(hints), CreateComparisonError.InvalidScalaVersionCase(_), _.invalidScalaVersion)
      val alt = schema.oneOf[CreateComparisonError]("InvalidScalaVersion")
    }

    trait Visitor[A] {
      def codeTooBig(value: CodeTooBig): A
      def invalidScalaVersion(value: InvalidScalaVersion): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def codeTooBig(value: CodeTooBig): A = default
        def invalidScalaVersion(value: InvalidScalaVersion): A = default
      }
    }

    implicit val schema: Schema[CreateComparisonError] = union(
      CreateComparisonError.CodeTooBigCase.alt,
      CreateComparisonError.InvalidScalaVersionCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[CreateComparisonError] = throwable match {
      case e: CodeTooBig => Some(CreateComparisonError.CodeTooBigCase(e))
      case e: InvalidScalaVersion => Some(CreateComparisonError.InvalidScalaVersionCase(e))
      case _ => None
    }
    def unliftError(e: CreateComparisonError): Throwable = e match {
      case CreateComparisonError.CodeTooBigCase(e) => e
      case CreateComparisonError.InvalidScalaVersionCase(e) => e
    }
  }
  final case class GetComparison(input: GetComparisonInput) extends MimaServiceOperation[GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing] = impl.getComparison(input.id)
    def ordinal: Int = 2
    def endpoint: smithy4s.Endpoint[MimaServiceOperation,GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing] = GetComparison
  }
  object GetComparison extends smithy4s.Endpoint[MimaServiceOperation,GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetComparisonInput, MimaServiceOperation.GetComparisonError, GetComparisonOutput, Nothing, Nothing] = Schema.operation(ShapeId("mimalyzer.protocol", "GetComparison"))
      .withInput(GetComparisonInput.schema)
      .withError(GetComparisonError.errorSchema)
      .withOutput(GetComparisonOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/api/comparison/{id}"), code = 200), smithy.api.Readonly())
    def wrap(input: GetComparisonInput): GetComparison = GetComparison(input)
  }
  sealed trait GetComparisonError extends scala.Product with scala.Serializable { self =>
    @inline final def widen: GetComparisonError = this
    def $ordinal: Int

    object project {
      def notFound: Option[NotFound] = GetComparisonError.NotFoundCase.alt.project.lift(self).map(_.notFound)
    }

    def accept[A](visitor: GetComparisonError.Visitor[A]): A = this match {
      case value: GetComparisonError.NotFoundCase => visitor.notFound(value.notFound)
    }
  }
  object GetComparisonError extends ErrorSchema.Companion[GetComparisonError] {

    def notFound(notFound: NotFound): GetComparisonError = NotFoundCase(notFound)

    val id: ShapeId = ShapeId("mimalyzer.protocol", "GetComparisonError")

    val hints: Hints = Hints.empty

    final case class NotFoundCase(notFound: NotFound) extends GetComparisonError { final def $ordinal: Int = 0 }

    object NotFoundCase {
      val hints: Hints = Hints.empty
      val schema: Schema[GetComparisonError.NotFoundCase] = bijection(NotFound.schema.addHints(hints), GetComparisonError.NotFoundCase(_), _.notFound)
      val alt = schema.oneOf[GetComparisonError]("NotFound")
    }

    trait Visitor[A] {
      def notFound(value: NotFound): A
    }

    object Visitor {
      trait Default[A] extends Visitor[A] {
        def default: A
        def notFound(value: NotFound): A = default
      }
    }

    implicit val schema: Schema[GetComparisonError] = union(
      GetComparisonError.NotFoundCase.alt,
    ){
      _.$ordinal
    }
    def liftError(throwable: Throwable): Option[GetComparisonError] = throwable match {
      case e: NotFound => Some(GetComparisonError.NotFoundCase(e))
      case _ => None
    }
    def unliftError(e: GetComparisonError): Throwable = e match {
      case GetComparisonError.NotFoundCase(e) => e
    }
  }
  final case class Health() extends MimaServiceOperation[Unit, Nothing, HealthOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[Unit, Nothing, HealthOutput, Nothing, Nothing] = impl.health()
    def ordinal: Int = 3
    def input: Unit = ()
    def endpoint: smithy4s.Endpoint[MimaServiceOperation,Unit, Nothing, HealthOutput, Nothing, Nothing] = Health
  }
  object Health extends smithy4s.Endpoint[MimaServiceOperation,Unit, Nothing, HealthOutput, Nothing, Nothing] {
    val schema: OperationSchema[Unit, Nothing, HealthOutput, Nothing, Nothing] = Schema.operation(ShapeId("mimalyzer.protocol", "Health"))
      .withInput(unit)
      .withOutput(HealthOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/api/health"), code = 200), smithy.api.Readonly())
    def wrap(input: Unit): Health = Health()
  }
}

