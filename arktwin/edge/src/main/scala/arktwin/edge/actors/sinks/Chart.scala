// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.{ChartAgent, ClockBase}
import arktwin.common.data.{TaggedDuration, TaggedTimestamp}
import arktwin.common.util.BehaviorsExtensions.*
import arktwin.common.util.CommonMessages.Nop
import arktwin.common.util.{MailboxConfig, VirtualTag}
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.actors.EdgeConfigurator.UpdateChartConfig
import arktwin.edge.actors.sinks.Clock.UpdateClockBase
import arktwin.edge.configs.ChartConfig
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.dispatch.ControlMessage

import scala.collection.mutable

/** An actor that manages transform data of neighbors.
  *
  * # Message Protocol
  *   - `DeleteExpiredNeighbors`: Delete expired neighbors
  *   - `Nop`: No operation
  *   - `Read`: Reads transform data of neighbors sorted by distance from first agents when edge
  *     culling is enabled
  *   - `UpdateChartConfig`: Updates the chart configuration
  *   - `UpdateClockBase`: Updates the clock base and distributes it to registered clock observers
  *   - `UpdateFirstAgents`: Updates first agents used as reference points for distance calculations
  *   - `UpdateNeighbor`: Updates neighbor and sorts neighbors by distance from first agents when
  *     edge culling is enabled
  */
object Chart:
  type Message = DeleteExpiredNeighbors.type | Nop.type | Read | UpdateChartConfig |
    UpdateClockBase | UpdateFirstAgents | UpdateNeighbor
  object DeleteExpiredNeighbors
  case class Read(replyTo: ActorRef[ReadReply]) extends ControlMessage
  case class UpdateFirstAgents(firstAgents: Seq[ChartAgent])
  case class UpdateNeighbor(neighbor: ChartAgent)

  case class CullingNeighbor(neighbor: ChartAgent, nearestDistance: Option[Double])
  case class ReadReply(orderedNeighbors: Seq[CullingNeighbor]) extends AnyVal

  def spawn(
      initClockBase: ClockBase,
      initChartConfig: ChartConfig
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initClockBase, initChartConfig),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      initClockBase: ClockBase,
      initChartConfig: ChartConfig
  ): Behavior[Message] = Behaviors.setupWithLogger: (context, logger) =>
    context.system.receptionist ! Receptionist.Register(
      Clock.observerKey,
      context.self
    )
    context.system.receptionist ! Receptionist.Register(
      EdgeConfigurator.chartObserverKey,
      context.self
    )

    var clockBase = initClockBase
    var chartConfig = initChartConfig
    val distances = mutable.Map[String, Double]()
    val orderedNeighbors = mutable.TreeMap[(Double, String), ChartAgent]()
    var firstAgents = Option(Seq[ChartAgent]())

    Behaviors.withTimers: timer =>
      if chartConfig.expiration.enabled then
        timer.startSingleTimer(
          DeleteExpiredNeighbors,
          DeleteExpiredNeighbors,
          chartConfig.expiration.checkMachineInterval
        )

      Behaviors.receiveMessage:
        case DeleteExpiredNeighbors =>
          val now = clockBase.now()
          val timeout = TaggedDuration.from[VirtualTag](chartConfig.expiration.timeout)
          orderedNeighbors.filterInPlace:
            case (_, agent) =>
              if now > agent.transform.timestamp.tagVirtual + timeout
              then
                distances -= agent.agentId
                logger.trace(
                  s"expired neighbor: ${agent.agentId}, timestamp: ${agent.transform.timestamp}, now: $now"
                )
                false
              else true
          if chartConfig.expiration.enabled then
            timer.startSingleTimer(
              DeleteExpiredNeighbors,
              DeleteExpiredNeighbors,
              chartConfig.expiration.checkMachineInterval
            )
          Behaviors.same

        case Nop =>
          Behaviors.same

        case Read(actorRef) =>
          actorRef ! ReadReply(orderedNeighbors.toSeq.map:
            case ((dist, _), agent) =>
              CullingNeighbor(agent, Some(dist).filter(_.isFinite)))
          Behaviors.same

        case UpdateClockBase(newClockBase) =>
          clockBase = newClockBase
          Behaviors.same

        case UpdateChartConfig(newChartConfig) =>
          if chartConfig.expiration != newChartConfig.expiration then
            if newChartConfig.expiration.enabled then
              timer.startSingleTimer(
                DeleteExpiredNeighbors,
                DeleteExpiredNeighbors,
                newChartConfig.expiration.checkMachineInterval
              )
            else timer.cancel(DeleteExpiredNeighbors)
          chartConfig = newChartConfig
          Behaviors.same

        case UpdateFirstAgents(newFirstAgents) =>
          if chartConfig.culling.enabled then
            if newFirstAgents.size <= chartConfig.culling.maxFirstAgents
            then firstAgents = Some(newFirstAgents)
            else
              if firstAgents.isDefined then
                logger.warn(
                  "edge culling is disabled because first agents is greater than arktwin.edge.dynamic.chart.culling.maxFirstAgents"
                )
              firstAgents = None
          else firstAgents = None
          Behaviors.same

        case UpdateNeighbor(neighbor) =>
          val oldDistance = distances.get(neighbor.agentId)
          val oldTimestamp = oldDistance
            .map(orderedNeighbors(_, neighbor.agentId).transform.timestamp.tagVirtual)
            .getOrElse(TaggedTimestamp.minValue[VirtualTag])
          if neighbor.transform.timestamp.tagVirtual >= oldTimestamp then
            for od <- oldDistance do orderedNeighbors -= ((od, neighbor.agentId))

            // TODO consider relative coordinates
            // TODO extrapolate first agents based on previous transforms?
            val distance = firstAgents
              .getOrElse(Seq())
              .map(a =>
                neighbor.transform.localTranslationMeter.distance(a.transform.localTranslationMeter)
              )
              .minOption
              .getOrElse(Double.PositiveInfinity)
            distances += neighbor.agentId -> distance
            orderedNeighbors += (distance, neighbor.agentId) -> neighbor
          Behaviors.same
