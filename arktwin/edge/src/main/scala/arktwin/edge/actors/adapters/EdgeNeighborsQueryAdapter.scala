// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.adapters

import arktwin.center.services.ClockBaseEx.*
import arktwin.center.services.{ClockBase, RegisterAgent}
import arktwin.common.MailboxConfig
import arktwin.common.data.DurationEx.*
import arktwin.common.data.TimestampEx.*
import arktwin.common.data.TransformEnuEx.*
import arktwin.edge.StaticEdgeConfig
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.actors.sinks.Chart.CullingAgent
import arktwin.edge.actors.sinks.{Chart, Clock, Register}
import arktwin.edge.data.{CoordinateConfig, Transform}
import arktwin.edge.endpoints.EdgeNeighborsQuery.{Request, Response, ResponseAgent}
import arktwin.edge.endpoints.{EdgeNeighborsQuery, NeighborChange}
import arktwin.edge.util.CommonMessages.Timeout
import arktwin.edge.util.{EdgeKamon, ErrorStatus, ServiceUnavailable}
import kamon.metric.Histogram
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.FiniteDuration
import scala.util.chaining.*

object EdgeNeighborsQueryAdapter:
  type Message = QueryMessage | CoordinateConfig | Report
  case class QueryMessage(
      request: Request,
      replyTo: ActorRef[Either[ErrorStatus, Response]]
  )
  case class Report(previousAgents: Set[String])

  def spawn(
      chart: ActorRef[Chart.Read],
      clock: ActorRef[Clock.Read],
      register: ActorRef[Register.Read],
      staticConfig: StaticEdgeConfig,
      initCoordinateConfig: CoordinateConfig,
      kamon: EdgeKamon
  ): ActorRef[ActorRef[Message]] => Spawn[Message] =
    Spawn(
      apply(chart, clock, register, staticConfig, initCoordinateConfig, kamon),
      getClass.getSimpleName,
      MailboxConfig(getClass.getName),
      _
    )

  def apply(
      chart: ActorRef[Chart.Read],
      clock: ActorRef[Clock.Read],
      register: ActorRef[Register.Read],
      staticConfig: StaticEdgeConfig,
      initCoordinateConfig: CoordinateConfig,
      kamon: EdgeKamon
  ): Behavior[Message] = Behaviors.setup: context =>
    context.system.receptionist ! Receptionist.Register(EdgeConfigurator.coordinateObserverKey, context.self)

    var coordinateConfig = initCoordinateConfig
    var optionalPreviousAgents: Option[Set[String]] = Some(Set())
    val simulationLatencyHistogram =
      kamon.restSimulationLatencyHistogram(EdgeNeighborsQuery.endpoint.showShort)

    Behaviors.receiveMessage:
      case QueryMessage(request, replyTo) =>
        (request.changeDetection, optionalPreviousAgents) match
          case (false, _) =>
            context.spawnAnonymous(
              child(
                request,
                None,
                replyTo,
                context.self,
                chart,
                clock,
                register,
                simulationLatencyHistogram,
                staticConfig.actorTimeout,
                coordinateConfig
              )
            )
            Behaviors.same

          case (true, Some(_)) =>
            context.spawnAnonymous(
              child(
                request,
                optionalPreviousAgents,
                replyTo,
                context.self,
                chart,
                clock,
                register,
                simulationLatencyHistogram,
                staticConfig.actorTimeout,
                coordinateConfig
              )
            )
            optionalPreviousAgents = None
            Behaviors.same

          case (true, None) =>
            replyTo ! Left(ServiceUnavailable("wait previous response"))
            Behaviors.same

      case Report(previousAgents) =>
        optionalPreviousAgents = Some(previousAgents)
        Behaviors.same

      case newCoordinateConfig: CoordinateConfig =>
        coordinateConfig = newCoordinateConfig
        Behaviors.same

  private type ChildMessage = Chart.ReadReply | ClockBase | Register.ReadReply | Timeout.type

  private def child(
      request: Request,
      optionalPreviousAgents: Option[Set[String]],
      replyTo: ActorRef[Either[ErrorStatus, Response]],
      parent: ActorRef[Report],
      chart: ActorRef[Chart.Read],
      clock: ActorRef[Clock.Read],
      register: ActorRef[Register.Read],
      simulationLatencyHistogram: Histogram,
      timeout: FiniteDuration,
      coordinateConfig: CoordinateConfig
  ): Behavior[ChildMessage] = Behaviors.setup: context =>
    context.setReceiveTimeout(timeout, Timeout)

    chart ! Chart.Read(context.self)
    var chartWait = Option.empty[Seq[CullingAgent]]

    clock ! Clock.Read(context.self)
    var clockWait = Option.empty[ClockBase]

    register ! Register.Read(context.self)
    var registerWait = Option.empty[Map[String, RegisterAgent]]

    def next(): Behavior[ChildMessage] = (chartWait, clockWait, registerWait) match
      case (Some(chartReply), Some(clockBase), Some(registerReply)) =>
        val requestTimestamp = request.timestamp.getOrElse(clockBase.now())
        val recognizedAgents = chartReply.view
          .flatMap: agent =>
            registerReply
              .get(agent.agent.agentId)
              .map: registerAgent =>
                simulationLatencyHistogram.record(
                  Math.max(0, (requestTimestamp - agent.agent.transform.timestamp).millisLong)
                )
                agent.agent.agentId -> ResponseAgent(
                  Some(Transform.fromEnu(agent.agent.transform.extrapolate(requestTimestamp), coordinateConfig)),
                  agent.nearestDistance,
                  Some(registerAgent.kind),
                  Some(registerAgent.status),
                  Some(registerAgent.assets),
                  optionalPreviousAgents.map: previousAgents =>
                    if previousAgents.contains(agent.agent.agentId)
                    then NeighborChange.Updated
                    else NeighborChange.Recognized
                )
          .pipe(request.neighborsNumber match
            case Some(n) => _.take(n)
            case None    => identity
          )
          .toSeq

        val agents = recognizedAgents.map(_._1).toSet

        if optionalPreviousAgents.nonEmpty then parent ! Report(agents)

        val unrecognizedAgents = optionalPreviousAgents
          .map: previousAgents =>
            (previousAgents &~ agents).map(
              _ -> ResponseAgent(None, None, None, None, None, Some(NeighborChange.Unrecognized))
            )
          .getOrElse(Set())

        replyTo ! Right(Response(requestTimestamp, (recognizedAgents ++ unrecognizedAgents).toMap))
        context.cancelReceiveTimeout()
        Behaviors.stopped

      case _ =>
        Behaviors.same

    Behaviors.receiveMessage:
      case Chart.ReadReply(value) =>
        chartWait = Some(value)
        next()

      case clockBase: ClockBase =>
        clockWait = Some(clockBase)
        next()

      case Register.ReadReply(value) =>
        registerWait = Some(value)
        next()

      case Timeout =>
        optionalPreviousAgents.foreach(parent ! Report(_))
        replyTo ! Left(ServiceUnavailable("timeout"))
        Behaviors.stopped
