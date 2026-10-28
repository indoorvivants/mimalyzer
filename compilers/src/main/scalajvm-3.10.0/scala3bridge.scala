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
import dotty.tools.dotc.util.SourceFile

import mimalyzer.iface._
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.jdk.CollectionConverters._
import java.util.stream.Collector
import java.util.stream.Collectors
import javax.script.CompiledScript

