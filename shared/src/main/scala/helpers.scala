package mimalyzer.protocol

def isScala3(sv: ScalaVersion) =
  sv.value.startsWith("3.")

def sortScalaVersions(l: List[ScalaVersion]) =
  l.sortBy:
    case s if s.value.startsWith("3.9")  => 1
    case s if s.value.startsWith("3.3")  => 2
    case s if s.value.startsWith("3.")   => 3
    case s if s.value.startsWith("2.13") => 4
    case s if s.value.startsWith("2.12") => 5
