//> using scala 2.13
//> using dep org.scala-lang:scala-compiler:2.13.15
//> using file ../compiler-interface/CompilationResult.java
//> using file ../compiler-interface/CompilerInterface.java
//> using file ../compiler-interface/CompilationError.java
//> using resourceDir ./resources

package mimalyzer.scala213

import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.Reporter
import scala.reflect.internal.util.Position
import scala.reflect.internal.util.SourceFile
import scala.reflect.internal.util.BatchSourceFile
import mimalyzer.iface._
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.jdk.CollectionConverters._
import java.util.stream.Collector
import java.util.stream.Collectors

case class Scala213CompilationError(line: Int, column: Int, msg: String)
    extends CompilationError

class AccumulatingReporter extends Reporter {

  private var errors = List.newBuilder[CompilationError]

  override protected def info0(
      pos: Position,
      msg: String,
      severity: Severity,
      force: Boolean
  ): Unit =
    if (severity == ERROR)
      errors.addOne(Scala213CompilationError(pos.line, pos.column, msg))

  def clear() = errors.clear()

  def getErrors() = errors.result().toArray

}

class Scala213CompilationResult(
    errs: Array[CompilationError],
    cpath: Array[String]
) extends CompilationResult {
  override def errors(): Array[CompilationError] = errs
  override def classpath(): Array[String] = cpath
}

class Scala213Compiler() extends CompilerInterface {
  val settings = new Settings()

  val reporter = new AccumulatingReporter

  def global() = new Global(settings, reporter)

  val savedClasspath = List.newBuilder[String]

  override def withClasspath(cp: Array[String]): CompilerInterface = {
    savedClasspath.addAll(cp)
    cp.foreach(s => settings.classpath.append(s))

    this
  }

  override def compile(
      fileName: String,
      contents: String,
      outDir: String
  ): CompilationResult = {
    Files.walk(Paths.get(outDir)).collect(Collectors.toList()).asScala.foreach {
      path =>
        if (path.getFileName().endsWith(".class"))
          Files.delete(path)
    }

    settings.outputDirs.setSingleOutput(outDir)
    reporter.clear()
    reporter.reset()
    val g = global()
    val run = new g.Run
    run.compileSources(List(new BatchSourceFile(fileName, contents)))

    g.close()

    new Scala213CompilationResult(
      reporter.getErrors(),
      savedClasspath.result.toArray
    )

  }
}
