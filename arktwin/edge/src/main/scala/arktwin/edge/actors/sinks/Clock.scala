/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.MailboxConfig
import arktwin.edge.StaticEdgeConfig
import arktwin.edge.actors.CommonMessages.Nop
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, MailboxSelector}
import org.apache.pekko.dispatch.ControlMessage

object Clock:
  type Message = Catch | Read | Nop.type
  case class Catch(clockBase: ClockBase)
  case class Read(replyTo: ActorRef[ClockBase]) extends ControlMessage

  def spawn(staticConfig: StaticEdgeConfig): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(staticConfig),
    getClass.getSimpleName,
    MailboxSelector.fromConfig(MailboxConfig.UnboundedControlAwareMailbox),
    _
  )

  def apply(staticConfig: StaticEdgeConfig): Behavior[Message] =
    Behaviors.withStash(staticConfig.bufferSize): buffer =>
      Behaviors.receiveMessage:
        case Catch(clockBase) =>
          buffer.unstashAll(active(clockBase))

        case message =>
          buffer.stash(message)
          Behaviors.same

  def active(initClockBase: ClockBase): Behavior[Message] = Behaviors.setup: context =>
    var clockBase = initClockBase

    Behaviors.receiveMessage:
      case Catch(newClockBase) =>
        clockBase = newClockBase
        context.log.info(newClockBase.toString)
        Behaviors.same

      case Read(replyTo) =>
        replyTo ! clockBase
        Behaviors.same

      case Nop =>
        Behaviors.same
