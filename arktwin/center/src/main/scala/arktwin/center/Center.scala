// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.center

import arktwin.center.actors.{Clock, Register, *}
import arktwin.center.configs.CenterConfig
import arktwin.center.services.*
import arktwin.center.util.{CenterKamon, CenterReporter}
import arktwin.common.util.{ErrorHandler, LoggerConfigurator}
import buildinfo.BuildInfo
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import org.apache.pekko.grpc.scaladsl.ServiceHandler
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.server.Directives.*
import org.apache.pekko.stream.BindFailedException
import org.apache.pekko.util.Timeout

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Random, Success}

object Center:
  def main(args: Array[String]): Unit =
    val config = CenterConfig.loadOrThrow()
    val rawConfig = CenterConfig.loadRawOrThrow()

    LoggerConfigurator.init(
      config.static.logLevel,
      config.static.logLevelColor,
      config.static.logSuppressionList
    )
    Kamon.init(rawConfig)

    given actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(
      Behaviors.setup[SpawnProtocol.Command](_ => SpawnProtocol()),
      BuildInfo.name,
      rawConfig
    )
    given ExecutionContextExecutor = actorSystem.executionContext
    given Scheduler = actorSystem.scheduler
    given Timeout = config.static.actorMachineTimeout

    scribe.info(BuildInfo.toString)
    scribe.info(config.toJson)
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
      val runServer = (port: Int) =>
        Http()
          .newServerAt(config.static.host, port)
          .bind(
            ServiceHandler.concatOrNotFound(
              AdminHandler.partial(AdminService(clock, register)),
              ChartPowerApiHandler.partial(ChartService(atlas, config.static, kamon)),
              ClockPowerApiHandler.partial(ClockService(clock, config.static)),
              RegisterPowerApiHandler.partial(RegisterService(register, config.static)),
              (
                path("")(getFromResource("root.html")) ~
                  path("metrics")(complete(PrometheusReporter.latestScrapeData())) ~
                  path("health")(complete("OK\n"))
              )(_)
            )
          )

      def runServerRecursively(port: Int): Future[ServerBinding] =
        if config.static.portAutoIncrement
        then
          if port <= (config.static.port + config.static.portAutoIncrementMax)
          then
            runServer(port).recoverWith:
              case _: BindFailedException =>
                runServerRecursively(port + 1)
          else
            Future(
              throw new BindFailedException:
                override def getMessage: String =
                  s"Bind failed on ${config.static.host} with port ${config.static.port}-${config.static.port + config.static.portAutoIncrementMax}"
            )
        else runServer(port)

      runServerRecursively(config.static.port).onComplete:
        case Success(server) =>
          scribe.info(s"running on ${server.localAddress.toString}")
        case Failure(e) =>
          ErrorHandler.flushAndExit(e)

  private def issueRunId(runIdPrefix: String): String =
    val characters = "0123456789abcdefghijklmnopqrstuvwxyz"
    val idSuffix = (0 until 7).map(_ => characters.toSeq(Random.nextInt(characters.size))).mkString
    (if runIdPrefix.isEmpty then "" else runIdPrefix + "-") + idSuffix
