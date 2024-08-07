// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center

import arktwin.center.actors.{Clock, Register, *}
import arktwin.center.services.*
import arktwin.center.util.CenterKamon
import arktwin.center.util.CenterReporter
import arktwin.common.LoggerConfigurator
import buildinfo.BuildInfo
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import org.apache.pekko.grpc.scaladsl.ServiceHandler
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.util.Random

object Center:
  def main(args: Array[String]): Unit =
    val config = CenterConfig.loadOrThrow()
    val rawConfig = CenterConfig.loadRawOrThrow()

    LoggerConfigurator.init(config.static.logLevel, config.static.logLevelColor)
    Kamon.init(rawConfig)

    given actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(
      Behaviors.setup[SpawnProtocol.Command](_ => SpawnProtocol()),
      BuildInfo.name,
      rawConfig
    )
    given ExecutionContextExecutor = actorSystem.executionContext
    given Scheduler = actorSystem.scheduler
    given Timeout = 10.seconds

    scribe.info(BuildInfo.toString)
    scribe.info(config.toString)
    scribe.debug(rawConfig.toString)

    val runId = issueRunId(config.static.runIdPrefix)
    scribe.info(s"Run ID: $runId")

    val kamon = CenterKamon(runId)
    val reporter = CenterReporter()
    Kamon.addReporter(reporter.getClass.getSimpleName, reporter)

    actorSystem ? DeadLetterListener.spawn(kamon)
    for
      atlas <- actorSystem ? Atlas.spawn(config.dynamic.atlas, kamon)
      clock <- actorSystem ? Clock.spawn(config.static.clock)
      register <- actorSystem ? Register.spawn(runId)
    do
      Http()
        .newServerAt(config.static.host, config.static.port)
        .bind(
          ServiceHandler.concatOrNotFound(
            AdminHandler.partial(AdminService(clock, register)),
            ChartPowerApiHandler.partial(
              ChartService(
                actorSystem.receptionist,
                atlas,
                config.static,
                kamon
              )
            ),
            ClockPowerApiHandler.partial(ClockService(actorSystem.receptionist, config.static)),
            RegisterPowerApiHandler.partial(
              RegisterService(register, actorSystem.receptionist, config.static)
            ),
            (
              path("")(getFromResource("root.html")) ~
                path("metrics")(complete(PrometheusReporter.latestScrapeData())) ~
                path("health")(complete("OK\n"))
            )(_)
          )
        )
        .foreach(server => scribe.info(server.localAddress.toString))

  private def issueRunId(runIdPrefix: String): String =
    val characters = "0123456789abcdefghijklmnopqrstuvwxyz"
    val idSuffix = (0 until 7).map(_ => characters.toSeq(Random.nextInt(characters.size))).mkString
    (if runIdPrefix.isEmpty then "" else runIdPrefix + "-") + idSuffix
