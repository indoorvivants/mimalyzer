package mimalyzer

import mimalyzer.protocol.*

case class CompilationJob(
    comparisonId: ComparisonId,
    jobId: JobId,
    codeBefore: ScalaCode,
    codeAfter: ScalaCode,
    scalaVersion: ScalaVersion
)
