addDependencyTreePlugin
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.4")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.github.sbt" % "sbt-license-report" % "1.7.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
addSbtPlugin("com.github.sbt" % "sbt-header" % "5.11.0")
addSbtPlugin("io.gatling" % "gatling-sbt" % "4.17.9")
addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.2.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.3.6"
)
