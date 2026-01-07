// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors

import arktwin.common.util.MailboxConfig
import arktwin.edge.configs.{ChartConfig, CoordinateConfig, EdgeConfig}
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist.Listing
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

/** An actor that manages and distributes edge configuration.
  *
  * # Message Protocol
  *   - `Listing`: Updates the list of registered observers
  *   - `Read`: Reads the current edge configuration
  *   - `UpdateCoordinateConfig`: Updates the coordinate configuration and distributes it to
  *     registered coordinate observers
  *   - `UpdateChartConfig`: Updates the chart configuration and distributes it to registered chart
  *     observers
  */
object EdgeConfigurator:
  type Message = Listing | Read | UpdateCoordinateConfig | UpdateChartConfig
  case class Read(replyTo: ActorRef[EdgeConfig])
  case class UpdateCoordinateConfig(config: CoordinateConfig) extends AnyVal
  case class UpdateChartConfig(config: ChartConfig) extends AnyVal

  val coordinateObserverKey: ServiceKey[UpdateCoordinateConfig] = ServiceKey(
    getClass.getName + "/" + UpdateCoordinateConfig.getClass.getName
  )
  val chartObserverKey: ServiceKey[UpdateChartConfig] = ServiceKey(
    getClass.getName + "/" + UpdateChartConfig.getClass.getName
  )

  def spawn(initEdgeConfig: EdgeConfig): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initEdgeConfig),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      initEdgeConfig: EdgeConfig
  ): Behavior[Message] = Behaviors.setup: context =>
    context.system.receptionist ! Receptionist.subscribe(coordinateObserverKey, context.self)
    context.system.receptionist ! Receptionist.subscribe(chartObserverKey, context.self)

    var edgeConfig = initEdgeConfig
    var coordinateObservers = Set[ActorRef[UpdateCoordinateConfig]]()
    var chartObservers = Set[ActorRef[UpdateChartConfig]]()

    Behaviors.receiveMessage:
      case coordinateObserverKey.Listing(newObservers) =>
        (newObservers &~ coordinateObservers).foreach(
          _ ! UpdateCoordinateConfig(edgeConfig.dynamic.coordinate)
        )
        coordinateObservers = newObservers
        Behaviors.same

      case chartObserverKey.Listing(newObservers) =>
        (newObservers &~ chartObservers).foreach(
          _ ! UpdateChartConfig(edgeConfig.dynamic.chart)
        )
        chartObservers = newObservers
        Behaviors.same

      case _: Listing =>
        Behaviors.unhandled

      case Read(replyTo) =>
        replyTo ! edgeConfig
        Behaviors.same

      case UpdateCoordinateConfig(coordinateConfig) =>
        coordinateObservers.foreach(_ ! UpdateCoordinateConfig(coordinateConfig))
        edgeConfig =
          edgeConfig.copy(dynamic = edgeConfig.dynamic.copy(coordinate = coordinateConfig))
        Behaviors.same

      case UpdateChartConfig(chartConfig) =>
        chartObservers.foreach(_ ! UpdateChartConfig(chartConfig))
        edgeConfig = edgeConfig.copy(dynamic = edgeConfig.dynamic.copy(chart = chartConfig))
        Behaviors.same
