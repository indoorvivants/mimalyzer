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
        val sv =
          if info.scala.startsWith("2.12") then ScalaVersion.SCALA_212
          else if info.scala.startsWith("2.13") then ScalaVersion.SCALA_213
          else if info.scala.startsWith("3.3") then ScalaVersion.SCALA_3_3_LTS
          else if info.scala.startsWith("3.9") then ScalaVersion.SCALA_3_9_LTS
          else ScalaVersion.SCALA_3_NEXT

        sv -> loadFromInfo(info)
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
