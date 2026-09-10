val Scala = "3.9.0"
val CompilerVersions = Seq("2.12.21", "2.13.18", "3.3.8", "3.9.0")

lazy val root = project
  .in(file("."))
  .aggregate(compilerInterface)
  .aggregate(compilers.projectRefs*)
  .aggregate(frontend)
  .aggregate(backend)

lazy val compilerInterface =
  project
    .in(file("compiler-interface"))
    .settings(autoScalaLibrary := false, crossPaths := false)

lazy val backend = project
  .in(file("backend"))
  .enablePlugins(RevolverPlugin)
  .dependsOn(shared.jvm(Scala), compilerInterface)
  .settings(
    scalaVersion := Scala,
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %% "tasty-mima" % "1.4.1",
      "ch.epfl.scala" %% "tasty-query" % "1.9.0",
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % "0.19.11",
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % "0.19.11",
      "com.indoorvivants" %% "decline-derive" % "0.3.6",
      "com.outr" %% "scribe-cats" % "3.19.0",
      "com.typesafe" %% "mima-core" % "1.2.0",
      "dev.rolang" %% "dumbo" % "0.10.2",
      "io.circe" %% "circe-jawn" % "0.14.16",
      "io.circe" %% "circe-parser" % "0.14.16",
      "org.http4s" %% "http4s-ember-server" % "0.23.36",
      "org.tpolecat" %% "skunk-core" % "1.0.0",
      "com.indoorvivants" %% "toml" % "0.3.0"
    ),
    (Compile / compile) := Def.uncached {
      (Compile / compile).dependsOn(Compile / copyResources).value
    },
    Compile / resourceGenerators += {
      val filter = ScopeFilter(
        inProjects(compilers.projectRefs*),
        inConfigurations(Compile)
      )
      Def.task {
        val allInfos = compilerInfo.all(filter).value
        val path =
          (Compile / managedResourceDirectories).value.head / "compilers.toml"

        def tomlify(arr: Iterable[String]) =
          arr.map(i => s"""  "$i" """.trim()).mkString("[", ", ", "]")

        val contents = allInfos
          .map { info =>
            s"""
      |[[compilers]]
      |scala = "${info.scala}"
      |bridgeClasspath = ${tomlify(info.bridgeClasspath)}
      |compilerClasspath = ${tomlify(info.compilerClasspath)}
      |libraryClasspath = ${tomlify(info.libraryClasspath)}
      """
          }
          .mkString("\n\n")

        IO.write(path, contents)

        Seq(path)
      }
    }
  )

lazy val shared = projectMatrix
  .in(file("shared"))
  .jsPlatform(Seq(Scala))
  .jvmPlatform(Seq(Scala))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value
    )
  )

lazy val frontend = project
  .in(file("frontend"))
  .dependsOn(shared.js(Scala))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalaVersion := Scala,
    libraryDependencies ++= Seq(
      "com.raquo" %% "laminar" % "17.2.1",
      "com.raquo" %% "waypoint" % "9.0.0",
      "tech.neander" %% "smithy4s-fetch" % "0.0.5"
    )
  )
  .enablePlugins(RevolverProcessPlugin)
  .settings(
    reStartCommand := Seq("npm", "run", "dev"),
    reStart / baseDirectory := (ThisBuild / baseDirectory).value / "frontend",
    scalaJSUseMainModuleInitializer := true
  )

lazy val compilers = projectMatrix
  .in(file("compilers"))
  .dependsOn(compilerInterface)
  .jvmPlatform(CrossVersion.full, CompilerVersions)
  .settings(
    Compile / unmanagedResourceDirectories ++= {
      val segments = scalaVersion.value.split("\\.")
      val names = (1 to 3)
        .map(len => segments.take(len).mkString("."))
        .map("resources-" + _)

      val base = (Compile / resourceDirectory).value.getParentFile()

      names.map(n => base / n)
    },
    libraryDependencies += {
      if scalaVersion.value.startsWith("3.") then
        "org.scala-lang" %% "scala3-compiler" % scalaVersion.value
      else "org.scala-lang" % "scala-compiler" % scalaVersion.value
    },
    scalacOptions ++= {
      if scalaVersion.value.startsWith("2.") then Seq("-Xsource:3")
      else Seq.empty
    },

    compilerInfo := {

      def getJars(mid: ModuleID) =

        val depRes = (update / dependencyResolution).value
        val updc = (update / updateConfiguration).value
        val uwconfig = (update / unresolvedWarningConfiguration).value
        val modDescr = depRes.wrapDependencyInModule(mid)
        val log = (streams).value.log

        depRes
          .update(
            modDescr,
            updc,
            uwconfig,
            log
          )
          .map(_.allFiles)
          .fold(uw => throw uw.resolveException, identity)
      end getJars

      val compilerModuleID =
        if scalaVersion.value.startsWith("3.") then
          "org.scala-lang" % "scala3-compiler_3" % scalaVersion.value
        else "org.scala-lang" % "scala-compiler" % scalaVersion.value

      val libraryModuleID =
        if scalaVersion.value.startsWith("3.") then
          "org.scala-lang" % "scala3-library_3" % scalaVersion.value
        else "org.scala-lang" % "scala-library" % scalaVersion.value

      (
        scala = scalaVersion.value,
        bridgeClasspath = (Compile / fullClasspath).value.toVector
          .map(_.data)
          .map(hv => fileConverter.value.toPath(hv).toString),
        compilerClasspath = getJars(compilerModuleID).map(_.toString),
        libraryClasspath = getJars(libraryModuleID).map(_.toString)
      )
    }
  )

@transient
val compilerInfo = taskKey[
  (
      scala: String,
      bridgeClasspath: Vector[String],
      compilerClasspath: Vector[String],
      libraryClasspath: Vector[String]
  )
]("")
