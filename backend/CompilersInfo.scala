package mimalyzer

import mimalyzer.iface.CompilerInterface
import java.io.File
import java.net.URL
import java.util.ServiceLoader
import mimalyzer.protocol.ScalaVersion

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
