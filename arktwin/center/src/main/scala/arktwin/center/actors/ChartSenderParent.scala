// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.common.MailboxConfig
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object ChartSenderParent:
  type Message = SpawnSender | ReadSenders
  case class SpawnSender(
      edgeId: String,
      subscriber: ActorRef[ChartSender.Subscribe]
  )
  case class ReadSenders(
      replyTo: ActorRef[ReadSendersReply]
  )

  case class ReadSendersReply(
      senders: Map[String, ActorRef[ChartSender.Message]]
  )

  def apply(): Behavior[Message] = Behaviors.setup: context =>
    Behaviors.receiveMessage:
      case SpawnSender(edgeId, subscriber) =>
        // TODO check actor name collision possibility when same edge reconnecting streams
        context.spawn(
          ChartSender(edgeId, subscriber),
          edgeId,
          MailboxConfig(ChartSender.getClass.getName)
        )
        Behaviors.same

      case ReadSenders(replyTo) =>
        replyTo ! ReadSendersReply(
          context.children.map(child => (child.path.name, child.unsafeUpcast[ChartSender.Message])).toMap
        )
        Behaviors.same
