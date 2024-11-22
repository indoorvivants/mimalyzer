package mimalyzer

import cats.effect.*
import com.typesafe.tools.mima.lib.MiMaLib
import fs2.io.file.Files
import fs2.io.process.Processes
import fullstack_scala.protocol.{CodeLabel, CompilationFailed, *}
import mimalyzer.iface.*
import tastymima.TastyMiMa
import tastymima.intf.Config

import java.nio.file.Paths
import scala.concurrent.ExecutionContext
import java.nio.file.Path
import java.nio.file.FileSystems
import java.net.URI
import tastyquery.jdk.ClasspathLoaders

val files = Files[IO]
val proc = Processes[IO]

extension (c: CompilationError)
  def render =
    s"[LINE=${c.line}, COLUMN=${c.column}] ${c.msg}"

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
    oldClasspath = javaLib ::: entryBefore +: classpathBefore
    newClasspath = javaLib ::: entryAfter +: classpathAfter

    _ <- scribe.cats.io.info(oldClasspath.mkString(":"))
    _ <- scribe.cats.io.info(newClasspath.mkString(":"))

    tastyProblems <- IO.blocking(
      Option.when(scalaVersion == ScalaVersion.SCALA_3_LTS):
        tastymima.analyze(
          oldClasspath = oldClasspath,
          oldClasspathEntry = entryBefore,
          newClasspath = newClasspath,
          newClasspathEntry = entryAfter
        )
    )

    _ <- files.deleteRecursively(classDirOld)
    _ <- files.deleteRecursively(classDirNew)
  yield Summary(
    mima = problems.map(p => Problem(Some(p.toString))),
    tasty = tastyProblems.toList.flatten.map(p => Problem(Some(p.toString)))
  )
  end for
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
