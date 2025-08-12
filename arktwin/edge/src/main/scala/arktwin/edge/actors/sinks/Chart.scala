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
import arktwin.edge.actors.EdgeConfigurator.UpdateCullingConfig
import arktwin.edge.configs.CullingConfig
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.dispatch.ControlMessage

import scala.collection.mutable

/** An actor that manages transform data of neighbors.
  *
  * # Message Protocol
  *   - `Read`: Reads transform data of neighbors sorted by distance from first agents when edge
  *     culling is enabled
  *   - `Update`: Updates transform data of neighbors sorted by distance from first agents when edge
  *     culling is enabled
  *   - `UpdateFirstAgents`: Updates first agents used as reference points for distance calculations
  *   - `UpdateCullingConfig`: Updates the culling configuration
  *   - `Nop`: No operation
  */
object Chart:
  type Message = Read | Update | UpdateFirstAgents | UpdateCullingConfig | Nop.type
  case class Read(replyTo: ActorRef[ReadReply]) extends ControlMessage
  case class Update(agent: ChartAgent)
  case class UpdateFirstAgents(agents: Seq[ChartAgent])

  case class ReadReply(orderedAgents: Seq[CullingAgent]) extends AnyVal
  case class CullingAgent(agent: ChartAgent, nearestDistance: Option[Double])

  def spawn(
      initCullingConfig: CullingConfig
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initCullingConfig),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      initCullingConfig: CullingConfig
  ): Behavior[Message] = Behaviors.setupWithLogger: (context, logger) =>
    context.system.receptionist ! Receptionist.Register(
      EdgeConfigurator.cullingObserverKey,
      context.self
    )

    var cullingConfig = initCullingConfig
    val distances = mutable.Map[String, Double]()
    val orderedAgents = mutable.TreeMap[(Double, String), ChartAgent]()
    var firstAgents = Option(Seq[ChartAgent]())

    Behaviors.receiveMessage:
      case Read(actorRef) =>
        actorRef ! ReadReply(orderedAgents.toSeq.map:
          case ((dist, _), agent) =>
            CullingAgent(agent, Some(dist).filter(_.isFinite)))
        Behaviors.same

      case Update(agent) =>
        val oldDistance = distances.get(agent.agentId)
        val oldTimestamp = oldDistance
          .map(orderedAgents(_, agent.agentId).transform.timestamp.tagVirtual)
          .getOrElse(TaggedTimestamp.minValue[VirtualTag])
        if agent.transform.timestamp.tagVirtual >= oldTimestamp then
          for od <- oldDistance do orderedAgents -= ((od, agent.agentId))

          // TODO consider relative coordinates
          // TODO extrapolate first agents based on previous transforms?
          val distance = firstAgents
            .getOrElse(Seq())
            .map(a =>
              agent.transform.localTranslationMeter.distance(a.transform.localTranslationMeter)
            )
            .minOption
            .getOrElse(Double.PositiveInfinity)
          distances += agent.agentId -> distance
          orderedAgents += (distance, agent.agentId) -> agent
        Behaviors.same

      case UpdateFirstAgents(newFirstAgents) =>
        if cullingConfig.edgeCulling then
          if newFirstAgents.size <= cullingConfig.maxFirstAgents
          then firstAgents = Some(newFirstAgents)
          else
            if firstAgents.nonEmpty then
              logger.warn(
                "edge culling is disabled because first agents is greater than arktwin.edge.culling.maxFirstAgents"
              )
            firstAgents = None
        else firstAgents = None
        Behaviors.same

      case UpdateCullingConfig(newCullingConfig) =>
        cullingConfig = newCullingConfig
        Behaviors.same

      case Nop =>
        Behaviors.same
