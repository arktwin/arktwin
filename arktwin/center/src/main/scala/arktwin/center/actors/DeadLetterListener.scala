// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.util.CenterKamon
import arktwin.common.MailboxConfig
import org.apache.pekko.actor.DeadLetter
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.eventstream.EventStream.Subscribe
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object DeadLetterListener:
  type Message = DeadLetter

  def spawn(kamon: CenterKamon): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(kamon),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(kamon: CenterKamon): Behavior[Message] = Behaviors.setup: context =>
    context.system.eventStream ! Subscribe(context.self)

    val deadLetterNumCounter = kamon.deadLetterNumCounter()

    Behaviors.receiveMessage:
      case DeadLetter(message, sender, recipient) =>
        deadLetterNumCounter
          .withTag(CenterKamon.recipientKey, recipient.path.toString())
          .increment()
        Behaviors.same
