// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ChartAgent
import arktwin.common.data.TaggedTimestamp
import arktwin.common.data.TimestampExtensions.*
import arktwin.common.data.Vector3EnuExtensions.*
import arktwin.common.util.BehaviorsExtensions.*
import arktwin.common.util.CommonMessages.Nop
import arktwin.common.util.{MailboxConfig, VirtualTag}
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.actors.EdgeConfigurator.UpdateChartConfig
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
  *   - `Nop`: No operation
  *   - `Read`: Reads transform data of neighbors sorted by distance from first agents when edge
  *     culling is enabled
  *   - `UpdateChartConfig`: Updates the chart configuration
  *   - `UpdateFirstAgents`: Updates first agents used as reference points for distance calculations
  *   - `UpdateNeighbor`: Updates neighbor and sorts neighbors by distance from first agents when
  *     edge culling is enabled
  */
object Chart:
  type Message = Nop.type | Read | UpdateChartConfig | UpdateFirstAgents | UpdateNeighbor
  case class Read(replyTo: ActorRef[ReadReply]) extends ControlMessage
  case class UpdateFirstAgents(firstAgents: Seq[ChartAgent])
  case class UpdateNeighbor(neighbor: ChartAgent)

  case class ReadReply(orderedNeighbors: Seq[CullingNeighbor]) extends AnyVal
  case class CullingNeighbor(neighbor: ChartAgent, nearestDistance: Option[Double])

  def spawn(
      initChartConfig: ChartConfig
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initChartConfig),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      initChartConfig: ChartConfig
  ): Behavior[Message] = Behaviors.setupWithLogger: (context, logger) =>
    context.system.receptionist ! Receptionist.Register(
      EdgeConfigurator.chartObserverKey,
      context.self
    )

    var chartConfig = initChartConfig
    val distances = mutable.Map[String, Double]()
    val orderedNeighbors = mutable.TreeMap[(Double, String), ChartAgent]()
    var firstAgents = Option(Seq[ChartAgent]())

    Behaviors.receiveMessage:
      case Nop =>
        Behaviors.same

      case Read(actorRef) =>
        actorRef ! ReadReply(orderedNeighbors.toSeq.map:
          case ((dist, _), agent) =>
            CullingNeighbor(agent, Some(dist).filter(_.isFinite)))
        Behaviors.same

      case UpdateChartConfig(newChartConfig) =>
        chartConfig = newChartConfig
        Behaviors.same

      case UpdateFirstAgents(newFirstAgents) =>
        if chartConfig.edgeCulling then
          if newFirstAgents.size <= chartConfig.maxFirstAgents
          then firstAgents = Some(newFirstAgents)
          else
            if firstAgents.isDefined then
              logger.warn(
                "edge culling is disabled because first agents is greater than arktwin.edge.dynamic.chart.maxFirstAgents"
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
