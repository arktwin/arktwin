// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.util.CenterKamon
import arktwin.common.MailboxConfig
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object ChartReceiverParent:
  type Message = SpawnReceiver | ReadReceivers
  case class SpawnReceiver(
      edgeId: String,
      initialForwarder: ChartReceiver.Forwarder,
      replyTo: ActorRef[ActorRef[ChartReceiver.Message]]
  )
  case class ReadReceivers(
      replyTo: ActorRef[ReadReceiversReply]
  )

  case class ReadReceiversReply(
      recorders: Map[String, ActorRef[ChartReceiver.Message]]
  )

  def apply(kamon: CenterKamon): Behavior[Message] = Behaviors.setup: context =>
    Behaviors.receiveMessage:
      case SpawnReceiver(edgeId, initialForwarder, replyTo) =>
        // TODO check actor name collision possibility when same edge reconnecting streams
        replyTo ! context.spawn(
          ChartReceiver(edgeId, initialForwarder, kamon),
          edgeId,
          MailboxConfig(ChartReceiver.getClass.getName)
        )
        Behaviors.same

      case ReadReceivers(replyTo) =>
        replyTo ! ReadReceiversReply(
          context.children.map(child => (child.path.name, child.unsafeUpcast[ChartReceiver.Message])).toMap
        )
        Behaviors.same
