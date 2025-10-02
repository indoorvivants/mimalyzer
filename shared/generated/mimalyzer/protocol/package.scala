package mimalyzer

package object protocol {
  type MimaService[F[_]] = smithy4s.kinds.FunctorAlgebra[MimaServiceGen, F]
  val MimaService = MimaServiceGen

  type ComparisonId = mimalyzer.protocol.ComparisonId.Type
  type ProblemsList = mimalyzer.protocol.ProblemsList.Type
  type ScalaCode = mimalyzer.protocol.ScalaCode.Type

}