// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.*
import arktwin.common.util.CommonMessages.Nop
import arktwin.common.util.MailboxConfig
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.dispatch.ControlMessage

import scala.collection.mutable

object Register:
  type Message = Catch | Get | Nop.type
  case class Catch(response: RegisterAgentSubscribe)
  case class Get(replyTo: ActorRef[ReadReply]) extends ControlMessage

  case class ReadReply(value: Map[String, RegisterAgent]) extends AnyVal

  def spawn(): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(): Behavior[Message] =
    val agents = mutable.Map[String, RegisterAgent]()

    Behaviors.receiveMessage:
      case Catch(r @ RegisterAgent(agentId, _, _, _)) =>
        agents += agentId -> r
        Behaviors.same

      case Catch(RegisterAgentUpdated(agentId, status)) =>
        for oldAgent <- agents.get(agentId) do
          agents(agentId) =
            RegisterAgent(agentId, oldAgent.kind, oldAgent.status ++ status, oldAgent.assets)
        Behaviors.same

      case Catch(RegisterAgentDeleted(agentId)) =>
        agents -= agentId
        Behaviors.same

      case Catch(RegisterAgentSubscribe.Empty) =>
        Behaviors.same

      case Get(replyTo) =>
        replyTo ! ReadReply(agents.toMap)
        Behaviors.same

      case Nop =>
        Behaviors.same
