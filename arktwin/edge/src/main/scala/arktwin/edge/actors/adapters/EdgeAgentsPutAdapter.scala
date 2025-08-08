// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.adapters

import arktwin.center.services.ClockBaseExtensions.*
import arktwin.center.services.{ChartAgent, ClockBase, RegisterAgentUpdated}
import arktwin.common.data.{MachineTimestamp, TransformEnu}
import arktwin.common.util.BehaviorsExtensions.*
import arktwin.common.util.CommonMessages.Timeout
import arktwin.common.util.{MailboxConfig, VirtualDurationHistogram}
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.actors.EdgeConfigurator.UpdateCoordinateConfig
import arktwin.edge.actors.sinks.{Chart, Clock}
import arktwin.edge.configs.{CoordinateConfig, StaticEdgeConfig}
import arktwin.edge.connectors.{ChartConnector, RegisterConnector}
import arktwin.edge.endpoints.EdgeAgentsPut
import arktwin.edge.endpoints.EdgeAgentsPut.{Request, Response}
import arktwin.edge.util.ErrorStatus.ServiceUnavailable
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object EdgeAgentsPutAdapter:
  type Message = Put | UpdateCoordinateConfig | Report
  case class Put(
      request: Request,
      restReceptionMachineTimestamp: MachineTimestamp,
      replyTo: ActorRef[Either[ErrorStatus, Response]]
  )
  case class Report(previousAgents: Map[String, TransformEnu])

  def spawn(
      chart: ActorRef[Chart.UpdateFirstAgents],
      chartPublish: ActorRef[ChartConnector.Publish],
      registerPublish: ActorRef[RegisterConnector.Publish],
      clock: ActorRef[Clock.Get],
      staticConfig: StaticEdgeConfig,
      initCoordinateConfig: CoordinateConfig,
      kamon: EdgeKamon
  ): ActorRef[ActorRef[Message]] => Spawn[Message] =
    Spawn(
      apply(chart, chartPublish, registerPublish, clock, staticConfig, initCoordinateConfig, kamon),
      getClass.getSimpleName,
      MailboxConfig(this),
      _
    )

  def apply(
      chart: ActorRef[Chart.UpdateFirstAgents],
      chartPublish: ActorRef[ChartConnector.Publish],
      registerPublish: ActorRef[RegisterConnector.Publish],
      clock: ActorRef[Clock.Get],
      staticConfig: StaticEdgeConfig,
      initCoordinateConfig: CoordinateConfig,
      kamon: EdgeKamon
  ): Behavior[Message] = Behaviors.setup: context =>
    context.system.receptionist ! Receptionist.Register(
      EdgeConfigurator.coordinateObserverKey,
      context.self
    )

    var coordinateConfig = initCoordinateConfig
    var optionalPreviousAgents: Option[Map[String, TransformEnu]] = Some(Map())
    val virtualLatencyHistogram =
      kamon.restVirtualLatencyHistogram(EdgeAgentsPut.endpoint.showShort)

    Behaviors.receiveMessage:
      // TODO filter registered agents
      case putMessage: Put =>
        optionalPreviousAgents match
          case Some(previousAgents) =>
            context.spawnAnonymous(
              session(
                putMessage,
                previousAgents,
                context.self,
                chart,
                chartPublish,
                registerPublish,
                clock,
                virtualLatencyHistogram,
                staticConfig,
                coordinateConfig
              )
            )
            optionalPreviousAgents = None
            Behaviors.same

          case None =>
            putMessage.replyTo ! Left(ServiceUnavailable("wait previous response"))
            Behaviors.same

        Behaviors.same

      case Report(previousAgents) =>
        optionalPreviousAgents = Some(previousAgents)
        Behaviors.same

      case UpdateCoordinateConfig(newCoordinateConfig) =>
        coordinateConfig = newCoordinateConfig
        Behaviors.same

  private type SessionMessage = ClockBase | Timeout.type

  private def session(
      putMessage: Put,
      previousAgents: Map[String, TransformEnu],
      parent: ActorRef[Report],
      chart: ActorRef[Chart.UpdateFirstAgents],
      chartPublish: ActorRef[ChartConnector.Publish],
      registerPublish: ActorRef[RegisterConnector.Publish],
      clock: ActorRef[Clock.Get],
      virtualLatencyHistogram: VirtualDurationHistogram,
      staticConfig: StaticEdgeConfig,
      coordinateConfig: CoordinateConfig
  ): Behavior[SessionMessage] = Behaviors.setupWithLogger: (context, logger) =>
    context.setReceiveTimeout(staticConfig.actorTimeout, Timeout)

    clock ! Clock.Get(context.self)
    var clockWait = Option.empty[ClockBase]

    def next(): Behavior[SessionMessage] = clockWait match
      case Some(clockBase) =>
        val now = clockBase.now()
        val requestTime = putMessage.request.timestamp.getOrElse(now)
        putMessage.replyTo ! Right(Response(now))
        virtualLatencyHistogram.record(now - requestTime)

        registerPublish ! RegisterConnector.Publish(
          putMessage.request.agents
            .flatMap((agentId, info) =>
              info.status.map(status => RegisterAgentUpdated(agentId, status))
            )
            .toSeq
        )

        val agents = putMessage.request.agents.flatMap: (agentId, info) =>
          info.transform.map: transform =>
            agentId -> ChartAgent(
              agentId,
              transform.normalize(requestTime, previousAgents.get(agentId), coordinateConfig)
            )
        parent ! Report(agents.view.mapValues(_.transform).toMap)
        chart ! Chart.UpdateFirstAgents(agents.values.toSeq)

        val chartPublishMessage = ChartConnector.Publish(
          agents.values.toSeq,
          putMessage.restReceptionMachineTimestamp
        )
        chartPublish ! chartPublishMessage
        logger.trace(
          s"put: ${putMessage.request}, chartPublish: $chartPublishMessage, previousAgents: $previousAgents"
        )

        context.cancelReceiveTimeout()
        Behaviors.stopped

      case None =>
        Behaviors.same

    Behaviors.receiveMessage:
      case clockBase: ClockBase =>
        clockWait = Some(clockBase)
        next()

      case Timeout =>
        parent ! Report(previousAgents)
        putMessage.replyTo ! Left(ServiceUnavailable("timeout"))
        Behaviors.stopped
