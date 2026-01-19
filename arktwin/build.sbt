import scalapb.GeneratorOption.FlatPackage

import scala.sys.process.Process

// avoid conflict with io.gatling.sbt.GatlingPlugin.autoImport.assembly
import sbtassembly.AssemblyPlugin.autoImport.assembly

val catsVersion = "2.13.0"
val gatlingVersion = "3.14.9"
val jsoniterScalaVersion = "2.38.8"
val kamonVersion = "2.8.0"
val pekkoHttpVersion = "1.3.0"
val pekkoVersion = "1.4.0"
val pureConfigVersion = "0.17.9"
val scalaTestVersion = "3.2.19"
val scribeVersion = "3.17.0"
val tapirSpecVersion = "0.11.10"
val tapirVersion = "1.13.5"

val apacheLicenseV2 = Some(
  HeaderLicense.Custom(
    """|SPDX-License-Identifier: Apache-2.0
     |Copyright 2024-2026 TOYOTA MOTOR CORPORATION
     |""".stripMargin
  )
)

inThisBuild(
  List(
    scalaVersion := "3.7.4",
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
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    licenseCheckAllow := Seq(
      LicenseCategory.Apache,
      LicenseCategory.BSD,
      LicenseCategory.MIT,
      LicenseCategory.Mozilla
    ),
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
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % jsoniterScalaVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % jsoniterScalaVersion,
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic-scala3" % pureConfigVersion,
      "com.outr" %% "scribe" % scribeVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version % "protobuf",
      "io.kamon" %% "kamon-core" % kamonVersion,
      "io.kamon" %% "kamon-testkit" % kamonVersion % Test,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    ),
    Compile / managedSourceDirectories += (Compile / pekkoGrpcCodeGeneratorSettings / target).value,
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
    assemblyMergeStrategy := {
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
    headerLicense := apacheLicenseV2,
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    libraryDependencies ++= Seq(
      "com.outr" %% "scribe-slf4j2" % scribeVersion,
      // kamon-prometheus depends on okio with module name problems via okhttp
      // see https://github.com/square/okio/issues/1306
      // prometheus page is delivered with pekko http instaed of okhttp
      "io.kamon" %% "kamon-prometheus" % kamonVersion exclude ("com.squareup.okhttp3", "okhttp"),
      "io.kamon" %% "kamon-system-metrics" % kamonVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-discovery" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-stream-typed" % pekkoVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.typelevel" %% "cats-core" % catsVersion
    )
  )

lazy val edge = (project in file("edge"))
  .dependsOn(common)
  .enablePlugins(BuildInfoPlugin, AutomateHeaderPlugin)
  .settings(
    name := "arktwin-edge",
    assemblyJarName := "arktwin-edge.jar",
    assemblyMergeStrategy := {
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
    Compile / unmanagedResourceDirectories += (viewer / baseDirectory).value / "dist",
    headerLicense := apacheLicenseV2,
    headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
    libraryDependencies ++= Seq(
      "com.outr" %% "scribe-slf4j2" % scribeVersion,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % tapirSpecVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-pekko-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % tapirVersion,
      // kamon-prometheus depends on okio with module name problems via okhttp
      // see https://github.com/square/okio/issues/1306
      // prometheus page is delivered with pekko http instaed of okhttp
      "io.kamon" %% "kamon-prometheus" % kamonVersion exclude ("com.squareup.okhttp3", "okhttp"),
      "io.kamon" %% "kamon-system-metrics" % kamonVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-discovery" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-stream-typed" % pekkoVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.typelevel" %% "cats-core" % catsVersion
    )
  )

lazy val viewer = (project in file("viewer")).settings(
  name := "arktwin-viewer",
  Keys.`package` := {
    val npmInstallExitCode = Process("npm install", baseDirectory.value).!
    require(npmInstallExitCode == 0, s"npm install failed with exit code $npmInstallExitCode.")
    val npmBuildExitCode = Process("npm run build", baseDirectory.value).!
    require(npmBuildExitCode == 0, s"npm run build failed with exit code $npmBuildExitCode.")
    baseDirectory.value / "dist"
  },
  run := {
    val npmRunDevExitCode = Process("npm run dev", baseDirectory.value).!
    require(npmRunDevExitCode == 0, s"npm run dev failed with exit code $npmRunDevExitCode.")
  }
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
