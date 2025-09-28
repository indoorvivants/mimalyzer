//> using scala 3.3.6
//> using dep org.scala-lang:scala3-compiler_3:3.3.6
//> using file ../compiler-interface/CompilationResult.java
//> using file ../compiler-interface/CompilerInterface.java
//> using file ../compiler-interface/CompilationError.java
//> using resourceDir ./resources

package mimalyzer.scala3

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts.{Context, FreshContext}
import dotty.tools.dotc.config.Settings.Setting._
import dotty.tools.dotc.interfaces
import dotty.tools.dotc.ast.Trees.Tree
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.interfaces.{SourceFile => ISourceFile}
import dotty.tools.dotc.interfaces.{Diagnostic => IDiagnostic}
import dotty.tools.dotc.reporting._
import dotty.tools.dotc.parsing.Parsers.Parser
import dotty.tools.dotc.Compiler
import dotty.tools.io.{AbstractFile, VirtualDirectory}
import dotty.tools.repl.AbstractFileClassLoader
import dotty.tools.dotc.util.SourceFile

import mimalyzer.iface._
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.jdk.CollectionConverters._
import java.util.stream.Collector
import java.util.stream.Collectors
import javax.script.CompiledScript

case class Scala3CompilationError(line: Int, column: Int, msg: String)
    extends CompilationError

class CompilerDriver(val settings: List[String]) extends Driver {

  /* Otherwise it will print usage instructions */
  override protected def sourcesRequired: Boolean = false

  def currentCtx = myInitCtx

  private val myInitCtx: Context = {
    val rootCtx = initCtx.fresh
    val ctx = setup(settings.toArray, rootCtx) match
      case Some((_, ctx)) => ctx
      case None           => rootCtx
    ctx.initialize()(using ctx)
    ctx
  }
}

class AccumulatingReporter extends Reporter {

  private var errors = List.newBuilder[CompilationError]
  override def doReport(dia: Diagnostic)(using Context) =
    if dia.level == interfaces.Diagnostic.ERROR then
      errors.addOne(
        Scala3CompilationError(dia.pos.line, dia.pos.column, dia.message)
      )

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

class Scala3Compiler() extends CompilerInterface {
  private val defaultFlags =
    List(
      "-color:never",
      "-unchecked",
      "-deprecation",
      "-Ximport-suggestion-timeout",
      "0"
    )

  // lazy val driver:  = CompilerDriver(defaultFlags)
  var driver: () => CompilerDriver = null

  val savedClasspath = List.newBuilder[String]

  override def withClasspath(cp: Array[String]): CompilerInterface = {
    savedClasspath.addAll(cp)
    driver = () => CompilerDriver(
      defaultFlags ++ List("-classpath", cp.mkString(File.pathSeparator))
    )

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

    val compiler = new Compiler
    val ctx = driver().currentCtx.fresh

    val reporter = new AccumulatingReporter

    val context = ctx
      .setReporter(reporter)
      .setSetting(
        ctx.settings.outputDir,
        AbstractFile.getDirectory(outDir)
      )

    val run = compiler.newRun(using context)
    val res =
      run.compileSources(List(SourceFile.virtual(fileName, contents)))

    new Scala213CompilationResult(
      reporter.getErrors(),
      savedClasspath.result.toArray
    )

  }
}
