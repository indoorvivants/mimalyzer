package mimalyzer

import java.net.{URL, URLClassLoader}

object CompilerClassLoader:
  def create(classpath: Array[URL], sc: Boolean = false): ClassLoader =
    new URLClassLoader(
      classpath,
      new FilteringClassLoader(getClass.getClassLoader(), sc)
    )
