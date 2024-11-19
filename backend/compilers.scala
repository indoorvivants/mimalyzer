package mimalyzer

import java.net.URLClassLoader
import java.net.URL
import java.io.File
import java.util.ServiceLoader
import mimalyzer.iface.CompilerInterface

final class FilteringClassLoader(parent: ClassLoader, sc: Boolean = false)
    extends ClassLoader(parent):
  private val parentPrefixes = Array(
    "java.",
    "javax.",
    "sun.reflect.",
    "jdk.internal.reflect.",
    "mimalyzer.",
    "sun.misc."
  ) ++ Option.when(sc)("scala.").toArray

  override def loadClass(name: String, resolve: Boolean): Class[?] =
    if parentPrefixes.exists(name.startsWith) then
      super.loadClass(name, resolve)
    else null
end FilteringClassLoader

object CompilerClassLoader:
  def create(classpath: Array[URL], sc: Boolean = false): ClassLoader =
    new URLClassLoader(
      classpath,
      new FilteringClassLoader(getClass.getClassLoader(), sc)
    )

opaque type Scala213Compiler <: CompilerInterface = CompilerInterface
opaque type Scala212Compiler <: CompilerInterface = CompilerInterface
opaque type Scala3Compiler <: CompilerInterface = CompilerInterface

case class Compilers(
    scala213: Scala213Compiler,
    scala212: Scala212Compiler,
    scala3: Scala3Compiler
)

object Scala3Compiler:
  def load(env: Map[String, String]): Scala3Compiler =
    val scala3lib = scala.io.Source
      .fromFile(env("SCALA_3_CLASSPATH_FILE"))
      .getLines()
      .mkString("\n")
    val bridge = env("SCALA_3_BRIDGE")

    val urls = (bridge :: scala3lib.split(":").toList)
      .map(new File(_))
      .map(_.toURL)

    val compilerLoader = CompilerClassLoader.create(urls.toArray, sc = true)
    val api = ServiceLoader
      .load(classOf[mimalyzer.iface.CompilerInterface], compilerLoader)
      .iterator()
      .next()

    api.withClasspath(scala3lib.split(":"))
  end load
end Scala3Compiler

object Scala212Compiler:
  def load(env: Map[String, String]): Scala212Compiler =
    val scala212Lib = scala.io.Source
      .fromFile(env("SCALA_212_CLASSPATH_FILE"))
      .getLines()
      .mkString("\n")
    val bridge = env("SCALA_212_BRIDGE")

    val urls = (bridge :: scala212Lib.split(":").toList)
      .map(new File(_))
      .map(_.toURL)

    val compilerLoader = CompilerClassLoader.create(urls.toArray)
    val api = ServiceLoader
      .load(classOf[mimalyzer.iface.CompilerInterface], compilerLoader)
      .iterator()
      .next()

    api.withClasspath(scala212Lib.split(":"))
  end load
end Scala212Compiler

object Scala213Compiler:
  def load(env: Map[String, String]): Scala213Compiler =
    val scala213Lib = scala.io.Source
      .fromFile(env("SCALA_213_CLASSPATH_FILE"))
      .getLines()
      .mkString("\n")
    val bridge = env("SCALA_213_BRIDGE")

    val urls = (bridge :: scala213Lib.split(":").toList)
      .map(new File(_))
      .map(_.toURL)

    val compilerLoader = CompilerClassLoader.create(urls.toArray)
    val api = ServiceLoader
      .load(classOf[mimalyzer.iface.CompilerInterface], compilerLoader)
      .iterator()
      .next()

    api.withClasspath(scala213Lib.split(":"))
  end load
end Scala213Compiler
