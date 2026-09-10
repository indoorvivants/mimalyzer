package mimalyzer.protocol

def isScala3(sv: ScalaVersion) = 
  sv match
    case ScalaVersion.SCALA_212 => false
    case ScalaVersion.SCALA_213 => false
    case ScalaVersion.SCALA_3_3_LTS => true
    case ScalaVersion.SCALA_3_9_LTS => true
    case ScalaVersion.SCALA_3_NEXT => true

