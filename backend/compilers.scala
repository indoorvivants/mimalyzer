package mimalyzer

import mimalyzer.iface.CompilerInterface

import java.io.File
import java.net.URL
import java.util.ServiceLoader

opaque type Scala213Compiler <: CompilerInterface = CompilerInterface
opaque type Scala212Compiler <: CompilerInterface = CompilerInterface
opaque type Scala3Compiler <: CompilerInterface = CompilerInterface

case class Compilers(
    scala213: Scala213Compiler,
    scala212: Scala212Compiler,
    scala3: Scala3Compiler
)

def loadCompiler[T <: CompilerInterface](
    compilerClasspathFileEnv: String,
    bridgeEnv: String,
    libraryEnv: String,
    sc: Boolean
)(env: Map[String, String]) =
  val lib = scala.io.Source
    .fromFile(env(libraryEnv))
    .getLines()
    .mkString("\n")

  val bridge = env(bridgeEnv)
  val compiler = scala.io.Source
    .fromFile(env(compilerClasspathFileEnv))
    .getLines()
    .mkString("\n")

  val urls = (bridge :: compiler.split(java.io.File.pathSeparator).toList)
    .map(new File(_))
    .map(_.toURL)

  val compilerLoader = CompilerClassLoader.create(urls.toArray, sc = sc)
  val api = ServiceLoader
    .load(classOf[mimalyzer.iface.CompilerInterface], compilerLoader)
    .iterator()
    .next()

  api.withClasspath(lib.split(":"))
end loadCompiler

object Scala3Compiler:
  def load(env: Map[String, String]): Scala3Compiler =
    loadCompiler(
      "SCALA_3_COMPILER_CLASSPATH_FILE",
      "SCALA_3_BRIDGE",
      "SCALA_3_CLASSPATH_FILE",
      false
    )(env)
end Scala3Compiler

object Scala212Compiler:
  def load(env: Map[String, String]): Scala212Compiler =
    loadCompiler(
      "SCALA_212_COMPILER_CLASSPATH_FILE",
      "SCALA_212_BRIDGE",
      "SCALA_212_CLASSPATH_FILE",
      false
    )(env)
end Scala212Compiler

object Scala213Compiler:
  def load(env: Map[String, String]): Scala213Compiler =
    loadCompiler(
      "SCALA_213_COMPILER_CLASSPATH_FILE",
      "SCALA_213_BRIDGE",
      "SCALA_213_CLASSPATH_FILE",
      false
    )(env)
  end load
end Scala213Compiler
