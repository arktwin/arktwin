addDependencyTreePlugin
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.13.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.12.0")
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.10.0")
addSbtPlugin("io.gatling" % "gatling-sbt" % "4.10.1")
addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.1.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.3.5"
)
