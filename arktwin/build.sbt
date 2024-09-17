import scalapb.GeneratorOption.FlatPackage

// avoid conflict with io.gatling.sbt.GatlingPlugin.autoImport.assembly
import sbtassembly.AssemblyPlugin.autoImport.assembly

val gatlingVersion = "3.12.0"
val jsoniterScalaVersion = "2.30.9"
val kamonVersion = "2.7.3"
val pekkoHttpVersion = "1.0.1"
val pekkoVersion = "1.1.1"
val pureConfigVersion = "0.17.7"
val scalaTestVersion = "3.2.19"
val scribeVersion = "3.15.0"
val tapirSpecVersion = "0.11.3"
val tapirVersion = "1.11.3"

val apacheLicenseV2 = Some(
  HeaderLicense.Custom(
    """|SPDX-License-Identifier: Apache-2.0
     |Copyright 2024 TOYOTA MOTOR CORPORATION
     |""".stripMargin
  )
)

inThisBuild(
  List(
    scalaVersion := "3.5.0",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xmax-inlines",
      "64",
      "-Wconf:src=pekko-grpc/.*:silent",
      "-Wunused:all"
    ),
    usePipelining := true,
    scalafixOnCompile := true,
    semanticdbEnabled := true,
    scalafmtOnCompile := true,
    headerEmptyLine := false,
    versionScheme := Some("semver-spec"),
    publish / skip := true,
    run / fork := true,
    Test / fork := true,
    outputStrategy := Some(StdoutOutput)
  )
)

lazy val root = (project in file("."))
  .aggregate(common, center, edge, e2e)
  .settings(
    name := "arktwin"
  )

lazy val common = (project in file("common"))
  .enablePlugins(PekkoGrpcPlugin, AutomateHeaderPlugin)
  .disablePlugins(AssemblyPlugin)
  .settings(
    name := "arktwin-common",
    headerLicense := apacheLicenseV2,
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      "com.outr" %% "scribe" % scribeVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version % "protobuf",
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    ),
    Compile / PB.targets +=
      scalapb.validate
        .gen(FlatPackage) -> (Compile / pekkoGrpcCodeGeneratorSettings / target).value,
    pekkoGrpcCodeGeneratorSettings += "server_power_apis"
  )

lazy val center = (project in file("center"))
  .dependsOn(common)
  .enablePlugins(BuildInfoPlugin, AutomateHeaderPlugin)
  .settings(
    name := "arktwin-center",
    assemblyJarName := "arktwin-center.jar",
    assembly / assemblyMergeStrategy := {
      case a if a.endsWith(".proto") =>
        MergeStrategy.discard
      // see https://github.com/sbt/sbt-assembly/issues/391
      case PathList("module-info.class") =>
        MergeStrategy.discard
      case PathList("META-INF", "versions", _, "module-info.class") =>
        MergeStrategy.discard
      case a =>
        (assembly / assemblyMergeStrategy).value(a)
    },
    assemblyExcludedJars := {
      (assembly / fullClasspath).value.filter { f =>
        // kamon-prometheus depends on okio with module name problems
        // see https://github.com/square/okio/issues/1306
        f.data.getName.startsWith("okio") && !f.data.getName.startsWith("okio-jvm")
      }
    },
    headerLicense := apacheLicenseV2,
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      "com.outr" %% "scribe" % scribeVersion,
      "com.outr" %% "scribe-slf4j2" % scribeVersion,
      "io.kamon" %% "kamon-prometheus" % kamonVersion,
      "io.kamon" %% "kamon-system-metrics" % kamonVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-discovery" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-stream-typed" % pekkoVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )

lazy val edge = (project in file("edge"))
  .dependsOn(common)
  .enablePlugins(BuildInfoPlugin, AutomateHeaderPlugin)
  .settings(
    name := "arktwin-edge",
    assemblyJarName := "arktwin-edge.jar",
    assembly / assemblyMergeStrategy := {
      case a if a.endsWith(".proto") =>
        MergeStrategy.discard
      // see https://github.com/sbt/sbt-assembly/issues/391
      case PathList("module-info.class") =>
        MergeStrategy.discard
      case PathList("META-INF", "versions", _, "module-info.class") =>
        MergeStrategy.discard
      // see https://tapir.softwaremill.com/en/latest/docs/openapi.html#using-swaggerui-with-sbt-assembly
      case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
        MergeStrategy.singleOrError
      case a =>
        (assembly / assemblyMergeStrategy).value(a)
    },
    assemblyExcludedJars := {
      (assembly / fullClasspath).value.filter { f =>
        // kamon-prometheus depends on okio with module name problems
        // see https://github.com/square/okio/issues/1306
        f.data.getName.startsWith("okio") && !f.data.getName.startsWith("okio-jvm")
      }
    },
    headerLicense := apacheLicenseV2,
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterScalaVersion % Provided,
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      "com.outr" %% "scribe" % scribeVersion,
      "com.outr" %% "scribe-slf4j2" % scribeVersion,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % tapirSpecVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-pekko-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % tapirVersion,
      "io.kamon" %% "kamon-prometheus" % kamonVersion,
      "io.kamon" %% "kamon-system-metrics" % kamonVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-discovery" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-stream-typed" % pekkoVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )

lazy val e2e = (project in file("e2e"))
  .enablePlugins(GatlingPlugin, AutomateHeaderPlugin)
  .settings(
    name := "arktwin-e2e",
    headerLicense := apacheLicenseV2,
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    libraryDependencies ++= Seq(
      "io.gatling" % "gatling-test-framework" % gatlingVersion % Test,
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % Test
    )
  )
