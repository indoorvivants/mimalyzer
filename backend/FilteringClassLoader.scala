package mimalyzer

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
