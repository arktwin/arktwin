/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.actors

import arktwin.center.services.ChartAgent
import arktwin.common.data.Timestamp
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object ChartSender:
  type Message = Subscribe | SubscriberTerminated.type
  object SubscriberTerminated
  case class Subscribe(agents: Seq[ChartAgent], forwardReceptionMachineTimestamp: Timestamp)

  def apply(edgeId: String, subscriber: ActorRef[Subscribe]): Behavior[Message] = Behaviors.setup: context =>
    context.watchWith(subscriber, SubscriberTerminated)

    Behaviors.receiveMessage:
      case subscribe: Subscribe =>
        subscriber ! subscribe
        Behaviors.same

      case SubscriberTerminated =>
        Behaviors.stopped
