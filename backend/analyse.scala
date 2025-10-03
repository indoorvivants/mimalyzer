package mimalyzer

import cats.effect.*
import com.typesafe.tools.mima.lib.MiMaLib
import fs2.io.file.Files
import fs2.io.process.Processes
import mimalyzer.iface.*
import mimalyzer.protocol.{CodeLabel, *}
import tastymima.TastyMiMa
import tastymima.intf.Config

import java.nio.file.{FileSystems, Path, Paths}
import scala.concurrent.ExecutionContext

import concurrent.duration.*

val files = Files[IO]
val proc = Processes[IO]

extension (c: CompilationError)
  def render =
    s"[LINE=${c.line}, COLUMN=${c.column}] ${c.msg}"

end extension

def analyseFileCode(
    oldScala: ScalaCode,
    newScala: ScalaCode,
    compiler: CompilerInterface,
    singleThreadExecutor: ExecutionContext,
    scalaVersion: ScalaVersion,
    progress: ProcessingStep => IO[Unit]
): IO[ComparisonResult] =
  val comp = for
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
      .timeoutTo(
        5.seconds,
        IO.raiseError(
          EarlyReturn(
            ComparisonResult
              .CompilationFailed(CodeLabel.BEFORE, "Took longer than 5 seconds")
          )
        )
      )

    _ <- progress(ProcessingStep.CODE_BEFORE_COMPILED)

    _ <- IO.raiseWhen(compiledOld.errors().nonEmpty)(
      EarlyReturn(
        ComparisonResult
          .CompilationFailed(
            CodeLabel.BEFORE,
            compiledOld.errors().map(_.render).mkString("\n")
          )
      )
    )

    compiledNew <- IO(
      compiler.compile(
        "new.scala",
        newScala.value,
        classDirNew.toNioPath.toAbsolutePath().toString()
      )
    )
      .evalOn(singleThreadExecutor)
      .timeoutTo(
        5.seconds,
        IO.raiseError(
          EarlyReturn(
            ComparisonResult
              .CompilationFailed(CodeLabel.AFTER, "Took longer than 5 seconds")
          )
        )
      )
    _ <- progress(ProcessingStep.CODE_AFTER_COMPILED)

    _ <- IO.raiseWhen(compiledNew.errors().nonEmpty)(
      EarlyReturn(
        ComparisonResult
          .CompilationFailed(
            CodeLabel.AFTER,
            compiledNew.errors().map(_.render).mkString("\n")
          )
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

    _ <- progress(ProcessingStep.MIMA_FINISHED)

    tastymima = new TastyMiMa(new Config)
    oldClasspath = javaLib ::: entryBefore +: classpathBefore
    newClasspath = javaLib ::: entryAfter +: classpathAfter

    tastyProblems <- IO.blocking(
      Option.when(scalaVersion == ScalaVersion.SCALA_3_LTS):
        tastymima.analyze(
          oldClasspath = oldClasspath,
          oldClasspathEntry = entryBefore,
          newClasspath = newClasspath,
          newClasspathEntry = entryAfter
        )
    )

    _ <- progress(ProcessingStep.TASTY_MIMA_FINISHED)

    _ <- files.deleteRecursively(classDirOld)
    _ <- files.deleteRecursively(classDirNew)
  yield ComparisonResult.Success(
    mimaProblems = MimaProblems(
      problems.map(p => Problem(p.description("new"), tag = Some(p.getClass.getSimpleName), symbol = p.matchName))
    ),
    tastyMimaProblems = TastyMimaProblems(
      tastyProblems.toList.flatten
        .map(p => Problem(p.getDescription(), tag = Some(p.kind.name()), symbol = Some(p.getPathString())))
    )
  )
  end comp

  comp.recover:
    case EarlyReturn(e) => e
end analyseFileCode

lazy val javaLib: List[Path] =
  System.getProperty("sun.boot.class.path") match
    case null =>
      List(
        FileSystems
          .getFileSystem(java.net.URI.create("jrt:/"))
          .getPath("modules", "java.base")
      )

    case bootClasspath =>
      val rtJarFile = bootClasspath
        .split(java.io.File.pathSeparatorChar)
        .find { path =>
          new java.io.File(path).getName() == "rt.jar"
        }
        .getOrElse {
          throw new RuntimeException(
            s"cannot find rt.jar in $bootClasspath"
          )
        }
      List(Paths.get(rtJarFile))
end javaLib
