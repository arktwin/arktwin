// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.DynamicCenterConfig.AtlasConfig
import arktwin.common.MailboxConfig
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, MailboxSelector}

object ChartRecorderParent:
  type Message = SpawnRecorder | ReadRecorders
  case class SpawnRecorder(
      edgeId: String,
      config: AtlasConfig,
      replyTo: ActorRef[ActorRef[ChartRecorder.Message]]
  )
  case class ReadRecorders(
      replyTo: ActorRef[ReadRecordersReply]
  )

  case class ReadRecordersReply(
      recorders: Map[String, ActorRef[ChartRecorder.Message]]
  )

  def apply(): Behavior[Message] = Behaviors.setup: context =>
    Behaviors.receiveMessage:
      case SpawnRecorder(edgeId, config, replyTo) =>
        // TODO check actor name collision possibility when same edge reconnecting streams
        replyTo ! context.spawn(
          ChartRecorder(edgeId, config),
          edgeId,
          MailboxSelector.fromConfig(MailboxConfig.UnboundedControlAwareMailbox)
        )
        Behaviors.same

      case ReadRecorders(replyTo) =>
        replyTo ! ReadRecordersReply(
          context.children.map(child => (child.path.name, child.unsafeUpcast[ChartRecorder.Message])).toMap
        )
        Behaviors.same
