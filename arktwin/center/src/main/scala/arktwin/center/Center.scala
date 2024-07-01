/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center

import arktwin.center.actors.{Clock, Register, *}
import arktwin.center.services.*
import arktwin.center.util.CenterReporter
import buildinfo.BuildInfo
import kamon.Kamon
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import org.apache.pekko.grpc.scaladsl.ServiceHandler
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object Center:
  def main(args: Array[String]): Unit =
    val config = CenterConfig.loadOrThrow()
    val rawConfig = CenterConfig.loadRawOrThrow()

    Kamon.init(rawConfig)

    given actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(
      Behaviors.setup[SpawnProtocol.Command](_ => SpawnProtocol()),
      BuildInfo.name,
      rawConfig
    )
    given ExecutionContextExecutor = actorSystem.executionContext
    given Scheduler = actorSystem.scheduler
    given Timeout = 10.seconds

    Kamon.addReporter(CenterReporter.getClass.getSimpleName, CenterReporter(actorSystem.log))

    actorSystem.log.info(BuildInfo.toString)
    actorSystem.log.info(config.toString)
    actorSystem.log.debug(rawConfig.toString)

    for
      atlas <- actorSystem ? Atlas.spawn(config.dynamic.atlas)
      clock <- actorSystem ? Clock.spawn(config.static.clock)
      register <- actorSystem ? Register.spawn(config.static.runIdPrefix)
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
                actorSystem.log
              )
            ),
            ClockPowerApiHandler.partial(ClockService(actorSystem.receptionist, config.static, actorSystem.log)),
            RegisterPowerApiHandler.partial(
              RegisterService(register, actorSystem.receptionist, config.static, actorSystem.log)
            )
          )
        )
        .foreach(server => actorSystem.log.info(server.localAddress.toString))
