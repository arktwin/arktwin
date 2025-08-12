// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.util.BehaviorsExtensions.*
import arktwin.common.util.CommonMessages.Nop
import arktwin.common.util.MailboxConfig
import arktwin.edge.configs.StaticEdgeConfig
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.Receptionist.Listing
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.dispatch.ControlMessage

/** An actor that manages and distributes clock base.
  *
  * # Message Protocol
  *   - `Listing`: Updates the list of registered observers
  *   - `Nop`: No operation
  *   - `Read`: Reads the current clock base
  *   - `UpdateClockBase`: Updates the clock base and distributes it to registered clock observers
  */
object Clock:
  type Message = Listing | Nop.type | Read | UpdateClockBase
  case class Read(replyTo: ActorRef[ClockBase]) extends ControlMessage
  case class UpdateClockBase(clockBase: ClockBase) extends AnyVal

  val observerKey: ServiceKey[UpdateClockBase] = ServiceKey(getClass.getName)

  def spawn(staticConfig: StaticEdgeConfig): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(staticConfig),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      staticConfig: StaticEdgeConfig
  ): Behavior[Message] = Behaviors.setup: context =>
    Behaviors.withStash(staticConfig.clockInitialStashSize): buffer =>
      context.system.receptionist ! Receptionist.subscribe(observerKey, context.self)

      Behaviors.receiveMessage:
        case UpdateClockBase(clockBase) =>
          buffer.unstashAll(active(clockBase))

        case message =>
          buffer.stash(message)
          Behaviors.same

  def active(
      initClockBase: ClockBase
  ): Behavior[Message] = Behaviors.withLogger: logger =>
    var clockBase = initClockBase
    logger.info(clockBase.toString)

    var observers = Set[ActorRef[UpdateClockBase]]()

    Behaviors.receiveMessage:
      case observerKey.Listing(newObservers) =>
        (newObservers &~ observers).foreach(_ ! UpdateClockBase(clockBase))
        observers = newObservers
        Behaviors.same

      case _: Listing =>
        Behaviors.unhandled

      case Nop =>
        Behaviors.same

      case Read(replyTo) =>
        replyTo ! clockBase
        Behaviors.same

      case UpdateClockBase(newClockBase) =>
        observers.foreach(_ ! UpdateClockBase(newClockBase))
        clockBase = newClockBase
        Behaviors.same
