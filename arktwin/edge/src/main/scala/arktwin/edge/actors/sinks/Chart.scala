// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ChartAgent
import arktwin.common.MailboxConfig
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.common.data.Vector3EnuEx.*
import arktwin.edge.DynamicEdgeConfig.CullingConfig
import arktwin.edge.actors.CommonMessages.Nop
import arktwin.edge.actors.EdgeConfigurator
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, MailboxSelector}
import org.apache.pekko.dispatch.ControlMessage

import scala.collection.mutable

object Chart:
  type Message = Catch | Read | FirstAgentsUpdate | CullingConfig | Nop.type
  case class Catch(agent: ChartAgent)
  case class Read(replyTo: ActorRef[ReadReply]) extends ControlMessage
  case class FirstAgentsUpdate(agents: Seq[ChartAgent])

  case class ReadReply(sortedAgents: Seq[CullingAgent]) extends AnyVal
  case class CullingAgent(agent: ChartAgent, nearestDistance: Option[Double])

  def spawn(
      initCullingConfig: CullingConfig
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initCullingConfig),
    getClass.getSimpleName,
    MailboxSelector.fromConfig(MailboxConfig.UnboundedControlAwareMailbox),
    _
  )

  def apply(
      initCullingConfig: CullingConfig
  ): Behavior[Message] = Behaviors.setup: context =>
    context.system.receptionist ! Receptionist.Register(EdgeConfigurator.cullingObserverKey, context.self)

    var cullingConfig = initCullingConfig
    val distances = mutable.Map[String, Double]()
    val orderedAgents = mutable.TreeMap[(Double, String), ChartAgent]()
    var firstAgents = Seq[ChartAgent]()

    Behaviors.receiveMessage:
      case Catch(agent) =>
        val oldDistance = distances.get(agent.agentId)
        val oldTimestamp = oldDistance
          .map(orderedAgents(_, agent.agentId).transform.timestamp)
          .getOrElse(Timestamp.minValue)
        if agent.transform.timestamp >= oldTimestamp then
          for od <- oldDistance do orderedAgents -= ((od, agent.agentId))

          // TODO consider relative coordinates
          // TODO extrapolate first agents based on previous transforms?
          val distance = firstAgents
            .map(a => agent.transform.localTranslationMeter.distance(a.transform.localTranslationMeter))
            .minOption
            .getOrElse(Double.PositiveInfinity)
          distances += agent.agentId -> distance
          orderedAgents += (distance, agent.agentId) -> agent
        Behaviors.same

      case Read(actorRef) =>
        actorRef ! ReadReply(orderedAgents.toSeq.map:
          case ((dist, _), agent) =>
            CullingAgent(agent, Some(dist).filter(_.isFinite))
        )
        Behaviors.same

      case FirstAgentsUpdate(newFirstAgents) =>
        if cullingConfig.edgeCulling then
          if newFirstAgents.size <= cullingConfig.maxFirstAgents then firstAgents = newFirstAgents
          else
            if firstAgents.nonEmpty then
              context.log.warn(
                "edge culling is disabled because first agents is greater than arktwin.edge.culling.maxFirstAgents"
              )
            firstAgents = Seq()
        else firstAgents = Seq()
        Behaviors.same

      case newCullingConfig: CullingConfig =>
        cullingConfig = newCullingConfig
        Behaviors.same

      case Nop =>
        Behaviors.same
