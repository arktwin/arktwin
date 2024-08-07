// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.actors.CommonMessages.*
import arktwin.center.services.*
import arktwin.common.MailboxConfig
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, Props}

import java.util.regex.PatternSyntaxException
import scala.collection.mutable
import scala.util.Random

object Register:
  type Message = EdgeCreate | AgentsCreate | AgentsUpdate | AgentsDelete | Receptionist.Listing | Complete.type |
    Failure
  case class EdgeCreate(request: EdgeCreateRequest, replyTo: ActorRef[EdgeCreateResponse])
  case class AgentsCreate(requests: EdgeAgentsPostRequests, replyTo: ActorRef[EdgeAgentsPostResponses])
  case class AgentsUpdate(request: RegisterAgentsPublish)
  case class AgentsDelete(agentSelector: AgentSelector)

  def spawn(runId: String): ActorRef[ActorRef[Message]] => Spawn[Message] =
    Spawn(apply(runId), getClass.getSimpleName, MailboxConfig(getClass.getName), _)

  val subscriberKey: ServiceKey[RegisterAgentsSubscribe] = ServiceKey(getClass.getName)

  // single substitution cipher so that simulators cannot depend on definitive IDs
  private val idSuffixCharacters = Random.shuffle("0123456789abcdefghijklmnopqrstuvwxyz")
  private def issueId(prefix: String, n: Int): String =
    var suffix = mutable.StringBuilder()
    var temp = n
    while (temp > 0)
      suffix.insert(0, idSuffixCharacters(temp % idSuffixCharacters.size))
      temp /= idSuffixCharacters.size
    (if (prefix.isEmpty()) "" else prefix + "-") + suffix.toString()

  private def apply(runId: String): Behavior[Message] = Behaviors.setup: context =>
    context.system.receptionist ! Receptionist.subscribe(subscriberKey, context.self)

    var edgeNum = 0
    var agentNum = 0
    val agents = mutable.Map[String, RegisterAgent]()
    var subscribers = Set[ActorRef[RegisterAgentsSubscribe]]()

    Behaviors.receiveMessage:
      case EdgeCreate(request, replyTo) =>
        edgeNum += 1
        val id = issueId(request.edgeIdPrefix, edgeNum)
        replyTo ! EdgeCreateResponse(id, runId)
        Behaviors.same

      case AgentsCreate(requests, replyTo) =>
        val newAgents = for request <- requests.requests yield
          agentNum += 1
          val id = issueId(request.agentIdPrefix, agentNum)
          RegisterAgent(id, request.kind, request.status, request.assets)
        replyTo ! EdgeAgentsPostResponses(newAgents.map(a => EdgeAgentsPostResponse(a.agentId)))
        for subscriber <- subscribers do subscriber ! RegisterAgentsSubscribe(newAgents)
        agents ++= newAgents.map(a => a.agentId -> a)
        Behaviors.same

      case AgentsUpdate(request) =>
        for subscriber <- subscribers do subscriber ! RegisterAgentsSubscribe(request.agents)
        for agent <- request.agents do
          for oldAgent <- agents.get(agent.agentId) do
            agents(agent.agentId) =
              RegisterAgent(agent.agentId, oldAgent.kind, oldAgent.status ++ agent.status, oldAgent.assets)
        Behaviors.same

      case AgentsDelete(agentSelector) =>
        val deletingIds = agentSelector match
          case AgentIdSelector(regex) =>
            try
              val r = regex.r
              agents.keys.filter(r.matches)
            catch
              case e: PatternSyntaxException =>
                context.log.warn(e.getMessage)
                Seq()
          case AgentKindSelector(regex) =>
            try
              val r = regex.r
              agents.filter(a => r.matches(a._2.kind)).keys
            catch
              case e: PatternSyntaxException =>
                context.log.warn(e.getMessage)
                Seq()
          case _ =>
            Seq()
        for subscriber <- subscribers do
          subscriber ! RegisterAgentsSubscribe(
            deletingIds.map(RegisterAgentDeleted.apply).toSeq
          )
        agents --= deletingIds
        Behaviors.same

      case subscriberKey.Listing(newSubscribers) =>
        for subscriber <- newSubscribers &~ subscribers do subscriber ! RegisterAgentsSubscribe(agents.values.toSeq)
        subscribers = newSubscribers
        Behaviors.same

      case _: Receptionist.Listing =>
        Behaviors.unhandled

      case Complete =>
        Behaviors.same

      case Failure(throwable, edgeId) =>
        Behaviors.same
