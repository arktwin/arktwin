// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors

import org.apache.pekko.actor.DeadLetter
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.eventstream.EventStream.Subscribe
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import arktwin.common.MailboxConfig
import arktwin.edge.util.EdgeKamon

object DeadLetterListener:
  type Message = DeadLetter

  def spawn(kamon: EdgeKamon): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(kamon),
    getClass.getSimpleName,
    MailboxConfig(getClass.getName),
    _
  )

  def apply(kamon: EdgeKamon): Behavior[Message] = Behaviors.setup: context =>
    context.system.eventStream ! Subscribe(context.self)

    val deadLetterNumCounter = kamon.deadLetterNumCounter()

    Behaviors.receiveMessage:
      case DeadLetter(message, sender, recipient) =>
        deadLetterNumCounter.withTag(EdgeKamon.recipientKey, recipient.path.toString()).increment()
        Behaviors.same
