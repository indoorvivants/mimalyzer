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
