package fullstack_scala.protocol

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.bijection
import smithy4s.schema.Schema.union

sealed trait ComparisonStatus extends scala.Product with scala.Serializable { self =>
  @inline final def widen: ComparisonStatus = this
  def $ordinal: Int

  object project {
    def waiting: Option[Waiting] = ComparisonStatus.WaitingCase.alt.project.lift(self).map(_.waiting)
    def processing: Option[Processing] = ComparisonStatus.ProcessingCase.alt.project.lift(self).map(_.processing)
    def failed: Option[CompilationFailed] = ComparisonStatus.FailedCase.alt.project.lift(self).map(_.failed)
    def completed: Option[Completed] = ComparisonStatus.CompletedCase.alt.project.lift(self).map(_.completed)
    def notFound: Option[NotFound] = ComparisonStatus.NotFoundCase.alt.project.lift(self).map(_.notFound)
  }

  def accept[A](visitor: ComparisonStatus.Visitor[A]): A = this match {
    case value: ComparisonStatus.WaitingCase => visitor.waiting(value.waiting)
    case value: ComparisonStatus.ProcessingCase => visitor.processing(value.processing)
    case value: ComparisonStatus.FailedCase => visitor.failed(value.failed)
    case value: ComparisonStatus.CompletedCase => visitor.completed(value.completed)
    case value: ComparisonStatus.NotFoundCase => visitor.notFound(value.notFound)
  }
}
object ComparisonStatus extends ShapeTag.Companion[ComparisonStatus] {

  def waiting(waiting: Waiting): ComparisonStatus = WaitingCase(waiting)
  def processing(processing: Processing): ComparisonStatus = ProcessingCase(processing)
  def failed(failed: CompilationFailed): ComparisonStatus = FailedCase(failed)
  def completed(completed: Completed): ComparisonStatus = CompletedCase(completed)
  def notFound(notFound: NotFound): ComparisonStatus = NotFoundCase(notFound)

  val id: ShapeId = ShapeId("fullstack_scala.protocol", "ComparisonStatus")

  val hints: Hints = Hints.empty

  final case class WaitingCase(waiting: Waiting) extends ComparisonStatus { final def $ordinal: Int = 0 }
  final case class ProcessingCase(processing: Processing) extends ComparisonStatus { final def $ordinal: Int = 1 }
  final case class FailedCase(failed: CompilationFailed) extends ComparisonStatus { final def $ordinal: Int = 2 }
  final case class CompletedCase(completed: Completed) extends ComparisonStatus { final def $ordinal: Int = 3 }
  final case class NotFoundCase(notFound: NotFound) extends ComparisonStatus { final def $ordinal: Int = 4 }

  object WaitingCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ComparisonStatus.WaitingCase] = bijection(Waiting.schema.addHints(hints), ComparisonStatus.WaitingCase(_), _.waiting)
    val alt = schema.oneOf[ComparisonStatus]("waiting")
  }
  object ProcessingCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ComparisonStatus.ProcessingCase] = bijection(Processing.schema.addHints(hints), ComparisonStatus.ProcessingCase(_), _.processing)
    val alt = schema.oneOf[ComparisonStatus]("processing")
  }
  object FailedCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ComparisonStatus.FailedCase] = bijection(CompilationFailed.schema.addHints(hints), ComparisonStatus.FailedCase(_), _.failed)
    val alt = schema.oneOf[ComparisonStatus]("failed")
  }
  object CompletedCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ComparisonStatus.CompletedCase] = bijection(Completed.schema.addHints(hints), ComparisonStatus.CompletedCase(_), _.completed)
    val alt = schema.oneOf[ComparisonStatus]("completed")
  }
  object NotFoundCase {
    val hints: Hints = Hints.empty
    val schema: Schema[ComparisonStatus.NotFoundCase] = bijection(NotFound.schema.addHints(hints), ComparisonStatus.NotFoundCase(_), _.notFound)
    val alt = schema.oneOf[ComparisonStatus]("notFound")
  }

  trait Visitor[A] {
    def waiting(value: Waiting): A
    def processing(value: Processing): A
    def failed(value: CompilationFailed): A
    def completed(value: Completed): A
    def notFound(value: NotFound): A
  }

  object Visitor {
    trait Default[A] extends Visitor[A] {
      def default: A
      def waiting(value: Waiting): A = default
      def processing(value: Processing): A = default
      def failed(value: CompilationFailed): A = default
      def completed(value: Completed): A = default
      def notFound(value: NotFound): A = default
    }
  }

  implicit val schema: Schema[ComparisonStatus] = union(
    ComparisonStatus.WaitingCase.alt,
    ComparisonStatus.ProcessingCase.alt,
    ComparisonStatus.FailedCase.alt,
    ComparisonStatus.CompletedCase.alt,
    ComparisonStatus.NotFoundCase.alt,
  ){
    _.$ordinal
  }.withId(id).addHints(hints)
}
