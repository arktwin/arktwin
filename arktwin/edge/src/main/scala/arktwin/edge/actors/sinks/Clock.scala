// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.util.BehaviorsExtensions.*
import arktwin.common.util.CommonMessages.Nop
import arktwin.common.util.MailboxConfig
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

  def spawn(initClockBase: ClockBase): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(initClockBase),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      initClockBase: ClockBase
  ): Behavior[Message] = Behaviors.setupWithLogger: (context, logger) =>
    context.system.receptionist ! Receptionist.subscribe(observerKey, context.self)

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
        logger.info(clockBase.toString)
        Behaviors.same
