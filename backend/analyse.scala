package mimalyzer

import cats.effect.*
import com.comcast.ip4s.Port
import com.comcast.ip4s.host
import org.http4s.ember.server.EmberServerBuilder

import scala.concurrent.duration.*
import fullstack_scala.protocol.*
import fs2.io.file.Files
import fs2.io.process.Processes, fs2.io.process.ProcessBuilder
import java.io.File

import com.typesafe.tools.mima.lib.MiMaLib
import cats.effect.std.Mutex
import scribe.cats.io as log

import fullstack_scala.protocol.{CompilationFailed, CodeLabel}

val files = Files[IO]
val proc = Processes[IO]

case class ProcOut(exitCode: Int, stdout: String, stderr: String)
object ProcOut:
  def collect(proc: fs2.io.process.Process[IO]) =
    import cats.syntax.all.*
    (
      proc.exitValue,
      proc.stdout.through(fs2.text.utf8Decode).compile.string,
      proc.stderr.through(fs2.text.utf8Decode).compile.string
    ).mapN(ProcOut.apply)

def analyseFileCode(
    oldScala: ScalaCode,
    newScala: ScalaCode,
    scalaVersion: ScalaVersion
) =
  for
    tmpdir1 <- files.createTempDirectory
    tmpdir2 <- files.createTempDirectory
    _ <- fs2
      .Stream(oldScala.value)
      .through(files.writeUtf8(tmpdir1.resolve("old.scala")))
      .compile
      .drain

    _ <- fs2
      .Stream(newScala.value)
      .through(files.writeUtf8(tmpdir2.resolve("new.scala")))
      .compile
      .drain

    proc1 <- proc
      .spawn(
        ProcessBuilder(
          "scala-cli",
          "compile",
          "old.scala",
          "-p",
          "--server=false",
          "-S",
          scalaVersion.value
        )
          .withWorkingDirectory(tmpdir1)
      )
      .use(ProcOut.collect)

    _ <- IO.raiseWhen(proc1.exitCode != 0)(
      CompilationFailed(CodeLabel.BEFORE, proc1.stderr)
    )

    proc2 <- proc
      .spawn(
        ProcessBuilder(
          "scala-cli",
          "compile",
          "new.scala",
          "-p",
          "--server=false",
          "-S",
          scalaVersion.value
        )
          .withWorkingDirectory(tmpdir2)
      )
      .use(ProcOut.collect)

    _ <- IO.raiseWhen(proc2.exitCode != 0)(
      CompilationFailed(CodeLabel.AFTER, proc2.stderr)
    )

    classes1 :: classpath1 = proc1.stdout.split(File.pathSeparatorChar).toList
    classes2 :: classpath2 = proc2.stdout.split(File.pathSeparatorChar).toList

    lib = new MiMaLib(classpath1.map(new File(_)))

    problems <- IO.blocking(
      lib.collectProblems(new File(classes1), new File(classes2), Nil)
    )

    _ <- files.deleteRecursively(tmpdir1)
    _ <- files.deleteRecursively(tmpdir2)
  yield problems.map(p => Problem(Some(p.toString)))
  end for
end analyseFileCode
