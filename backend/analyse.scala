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
import mimalyzer.iface.*
import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext

val files = Files[IO]
val proc = Processes[IO]

extension (c: CompilationError)
  def render =
    s"[LINE=${c.line}, COLUMN=${c.column}] ${c.msg}"

def analyseFileCode(
    oldScala: ScalaCode,
    newScala: ScalaCode,
    compiler: CompilerInterface,
    singleThreadExecutor: ExecutionContext
) =
  for
    tmpdir1 <- files.createTempDirectory
    tmpdir2 <- files.createTempDirectory

    proc1 <- IO(
      compiler.compile(
        "old.scala",
        oldScala.value,
        tmpdir1.toNioPath.toAbsolutePath().toString()
      )
    )
      .evalOn(singleThreadExecutor)

    proc2 <- IO(
      compiler.compile(
        "new.scala",
        newScala.value,
        tmpdir2.toNioPath.toAbsolutePath().toString()
      )
    )
      .evalOn(singleThreadExecutor)

    _ <- IO.raiseWhen(proc1.errors().nonEmpty)(
      CompilationFailed(
        CodeLabel.BEFORE,
        proc1.errors().map(_.render).mkString("\n")
      )
    )

    _ <- IO.raiseWhen(proc2.errors().nonEmpty)(
      CompilationFailed(
        CodeLabel.AFTER,
        proc2.errors().map(_.render).mkString("\n")
      )
    )

    lib = new MiMaLib(proc1.classpath().map(new File(_)))

    problems <- IO.blocking(
      lib.collectProblems(
        tmpdir1.toNioPath.toFile(),
        tmpdir2.toNioPath.toFile(),
        Nil
      )
    )

    _ <- files.deleteRecursively(tmpdir1)
    _ <- files.deleteRecursively(tmpdir2)
  yield problems.map(p => Problem(Some(p.toString)))
  end for
end analyseFileCode
