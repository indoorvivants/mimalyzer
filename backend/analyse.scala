package mimalyzer

import cats.effect.*
import com.typesafe.tools.mima.lib.MiMaLib
import fs2.io.file.Files
import fs2.io.process.Processes
import fullstack_scala.protocol.{CodeLabel, CompilationFailed, *}
import mimalyzer.iface.*

import java.io.File
import scala.concurrent.ExecutionContext
import tastymima.TastyMiMa
import tastymima.intf.Config
import java.nio.file.Paths

val files = Files[IO]
val proc = Processes[IO]

extension (c: CompilationError)
  def render =
    s"[LINE=${c.line}, COLUMN=${c.column}] ${c.msg}"

// case class CompiledFile(
//   output: java.nio.file.Path,
//   classpath: Array[java.nio.file.Path]
// )

// def mima(before: CompiledFile, after: CompiledFile) =
//   val lib = new MiMaLib(before.classpath.map(_.toFile))
//   for
//     problems <- IO.blocking(
//       lib.collectProblems(
//         before.output.toFile,
//         after.output.toFile,
//         Nil
//       )
//     )
//   yield problems.map(p => Problem(Some(p.toString)))
//   end for

case class Summary(mima: List[Problem], tasty: List[Problem])

def analyseFileCode(
    oldScala: ScalaCode,
    newScala: ScalaCode,
    compiler: CompilerInterface,
    singleThreadExecutor: ExecutionContext,
    scalaVersion: ScalaVersion
): IO[Summary] =
  for
    classDirOld <- files.createTempDirectory
    classDirNew <- files.createTempDirectory

    compiledOld <- IO(
      compiler.compile(
        "old.scala",
        oldScala.value,
        classDirOld.toNioPath.toAbsolutePath().toString()
      )
    )
      .evalOn(singleThreadExecutor)

    compiledNew <- IO(
      compiler.compile(
        "new.scala",
        newScala.value,
        classDirNew.toNioPath.toAbsolutePath().toString()
      )
    )
      .evalOn(singleThreadExecutor)

    _ <- IO.raiseWhen(compiledOld.errors().nonEmpty)(
      CompilationFailed(
        CodeLabel.BEFORE,
        compiledOld.errors().map(_.render).mkString("\n")
      )
    )

    _ <- IO.raiseWhen(compiledNew.errors().nonEmpty)(
      CompilationFailed(
        CodeLabel.AFTER,
        compiledNew.errors().map(_.render).mkString("\n")
      )
    )

    entryBefore = classDirOld.toNioPath
    entryAfter = classDirNew.toNioPath

    classpathBefore = compiledOld.classpath().map(Paths.get(_)).toList
    classpathAfter = compiledNew.classpath().map(Paths.get(_)).toList

    mima = new MiMaLib(classpathBefore.map(_.toFile))

    problems <- IO.blocking(
      mima.collectProblems(
        entryBefore.toFile(),
        entryAfter.toFile(),
        Nil
      )
    )

    tastymima = new TastyMiMa(new Config)

    _ = assert(java.nio.file.Files.exists(entryAfter.resolve("X.class")))
    _ = assert(java.nio.file.Files.exists(entryAfter.resolve("X.tasty")))

    tastyProblems <- IO.blocking(
      Option.when(scalaVersion == ScalaVersion.SCALA_3_LTS):
        tastymima.analyze(
          oldClasspath = entryBefore +: classpathBefore,
          oldClasspathEntry = entryBefore, 
          newClasspath = entryAfter +: classpathAfter,
          newClasspathEntry = entryAfter
        )
    )

    _ = println(classDirOld)
    _ = println(classDirNew)

    // _ <- files.deleteRecursively(classDirOld)
    // _ <- files.deleteRecursively(classDirNew)
  yield Summary(
    mima = problems.map(p => Problem(Some(p.toString))),
    tasty = tastyProblems.toList.flatten.map(p => Problem(Some(p.toString)))
  )
  end for
end analyseFileCode
