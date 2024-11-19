package fullstack_scala

package object protocol {
  type MimaService[F[_]] = smithy4s.kinds.FunctorAlgebra[MimaServiceGen, F]
  val MimaService = MimaServiceGen

  type ComparisonId = fullstack_scala.protocol.ComparisonId.Type
  type ProblemsList = fullstack_scala.protocol.ProblemsList.Type
  type ScalaCode = fullstack_scala.protocol.ScalaCode.Type

}