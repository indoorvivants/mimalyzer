package fullstack_scala.protocol

import smithy4s.Endpoint
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.Service
import smithy4s.ShapeId
import smithy4s.Transformation
import smithy4s.kinds.PolyFunction5
import smithy4s.kinds.toPolyFunction5.const5
import smithy4s.schema.OperationSchema

trait MimaServiceGen[F[_, _, _, _, _]] {
  self =>

  def getComparison(id: ComparisonId): F[GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing]
  def createComparison(attributes: ComparisonAttributes): F[CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing]

  def transform: Transformation.PartiallyApplied[MimaServiceGen[F]] = Transformation.of[MimaServiceGen[F]](this)
}

object MimaServiceGen extends Service.Mixin[MimaServiceGen, MimaServiceOperation] {

  val id: ShapeId = ShapeId("fullstack_scala.protocol", "MimaService")
  val version: String = "1.0.0"

  val hints: Hints = Hints(
    alloy.SimpleRestJson(),
  ).lazily

  def apply[F[_]](implicit F: Impl[F]): F.type = F

  object ErrorAware {
    def apply[F[_, _]](implicit F: ErrorAware[F]): F.type = F
    type Default[F[+_, +_]] = Constant[smithy4s.kinds.stubs.Kind2[F]#toKind5]
  }

  val endpoints: Vector[smithy4s.Endpoint[MimaServiceOperation, ?, ?, ?, ?, ?]] = Vector(
    MimaServiceOperation.GetComparison,
    MimaServiceOperation.CreateComparison,
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

}

sealed trait MimaServiceOperation[Input, Err, Output, StreamedInput, StreamedOutput] {
  def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[Input, Err, Output, StreamedInput, StreamedOutput]
  def ordinal: Int
  def input: Input
  def endpoint: Endpoint[MimaServiceOperation, Input, Err, Output, StreamedInput, StreamedOutput]
}

object MimaServiceOperation {

  object reified extends MimaServiceGen[MimaServiceOperation] {
    def getComparison(id: ComparisonId): GetComparison = GetComparison(GetComparisonInput(id))
    def createComparison(attributes: ComparisonAttributes): CreateComparison = CreateComparison(CreateComparisonInput(attributes))
  }
  class Transformed[P[_, _, _, _, _], P1[_ ,_ ,_ ,_ ,_]](alg: MimaServiceGen[P], f: PolyFunction5[P, P1]) extends MimaServiceGen[P1] {
    def getComparison(id: ComparisonId): P1[GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing] = f[GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing](alg.getComparison(id))
    def createComparison(attributes: ComparisonAttributes): P1[CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing] = f[CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing](alg.createComparison(attributes))
  }

  def toPolyFunction[P[_, _, _, _, _]](impl: MimaServiceGen[P]): PolyFunction5[MimaServiceOperation, P] = new PolyFunction5[MimaServiceOperation, P] {
    def apply[I, E, O, SI, SO](op: MimaServiceOperation[I, E, O, SI, SO]): P[I, E, O, SI, SO] = op.run(impl) 
  }
  final case class GetComparison(input: GetComparisonInput) extends MimaServiceOperation[GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing] = impl.getComparison(input.id)
    def ordinal: Int = 0
    def endpoint: smithy4s.Endpoint[MimaServiceOperation,GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing] = GetComparison
  }
  object GetComparison extends smithy4s.Endpoint[MimaServiceOperation,GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing] {
    val schema: OperationSchema[GetComparisonInput, Nothing, GetComparisonOutput, Nothing, Nothing] = Schema.operation(ShapeId("fullstack_scala.protocol", "GetComparison"))
      .withInput(GetComparisonInput.schema)
      .withOutput(GetComparisonOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("GET"), uri = smithy.api.NonEmptyString("/api/comparison/{id}"), code = 200), smithy.api.Readonly())
    def wrap(input: GetComparisonInput): GetComparison = GetComparison(input)
  }
  final case class CreateComparison(input: CreateComparisonInput) extends MimaServiceOperation[CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing] {
    def run[F[_, _, _, _, _]](impl: MimaServiceGen[F]): F[CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing] = impl.createComparison(input.attributes)
    def ordinal: Int = 1
    def endpoint: smithy4s.Endpoint[MimaServiceOperation,CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing] = CreateComparison
  }
  object CreateComparison extends smithy4s.Endpoint[MimaServiceOperation,CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing] {
    val schema: OperationSchema[CreateComparisonInput, Nothing, CreateComparisonOutput, Nothing, Nothing] = Schema.operation(ShapeId("fullstack_scala.protocol", "CreateComparison"))
      .withInput(CreateComparisonInput.schema)
      .withOutput(CreateComparisonOutput.schema)
      .withHints(smithy.api.Http(method = smithy.api.NonEmptyString("PUT"), uri = smithy.api.NonEmptyString("/api/comparison"), code = 200), smithy.api.Idempotent())
    def wrap(input: CreateComparisonInput): CreateComparison = CreateComparison(input)
  }
}

