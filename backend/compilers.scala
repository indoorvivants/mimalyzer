package mimalyzer

import mimalyzer.iface.CompilerInterface

import java.io.File
import java.net.URL
import java.util.ServiceLoader
import mimalyzer.protocol.ScalaVersion

case class CompilerInfo(
    scala: String,
    bridgeClasspath: List[String],
    compilerClasspath: List[String],
    libraryClasspath: List[String] = List.empty
) derives toml.Codec

case class CompilersInfo(
    compilers: List[CompilerInfo]
) derives toml.Codec

object CompilersInfo:
  def readFromResources =
    val l = scala.io.Source
      .fromInputStream(getClass().getResourceAsStream("/compilers.toml"))
      .getLines()
      .mkString("\n")
    toml.Toml.parseAs[CompilersInfo](l)

case class Compilers(
    mapping: Map[ScalaVersion, CompilerInterface]
)
object Compilers:
  def load(infos: CompilersInfo): Compilers =
    Compilers(
      infos.compilers.map { info =>
        ScalaVersion(info.scala) -> loadFromInfo(info)
      }.toMap
    )
end Compilers

def loadFromInfo(ci: CompilerInfo): CompilerInterface =
  val urls = (ci.bridgeClasspath.toArray ++ ci.compilerClasspath.toArray)
    .map(new File(_))
    .map(_.toURL)

  val compilerLoader = CompilerClassLoader.create(urls.toArray, sc = false)
  val api = ServiceLoader
    .load(classOf[mimalyzer.iface.CompilerInterface], compilerLoader)
    .iterator()
    .next()

  api.withClasspath(ci.libraryClasspath.toArray)
end loadFromInfo
