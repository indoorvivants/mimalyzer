package mimalyzer

import mimalyzer.protocol.{CodeLabel, *}

enum ComparisonResult:
  case CompilationFailed(which: CodeLabel, errorOut: String)
  case Success(
      mimaProblems: MimaProblems,
      tastyMimaProblems: TastyMimaProblems
  )
  case Failure(msg: String)
