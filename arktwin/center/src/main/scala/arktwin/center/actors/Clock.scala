/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.actors

import arktwin.center.StaticCenterConfig.ClockConfig
import arktwin.center.StaticCenterConfig.ClockConfig.Start.{Absolute, Relative, Schedule}
import arktwin.center.services.ClockBase
import arktwin.center.services.ClockBaseEx.*
import arktwin.common.MailboxConfig
import arktwin.common.data.DurationEx.*
import arktwin.common.data.TimestampEx.*
import arktwin.common.data.{Duration, Timestamp}
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, MailboxSelector}
import org.apache.pekko.dispatch.ControlMessage

import scala.concurrent.duration.DurationDouble

object Clock:
  type Message = SpeedUpdate | Receptionist.Listing
  case class SpeedUpdate(clockSpeed: Double) extends ControlMessage

  val subscriberKey: ServiceKey[ClockBase] = ServiceKey(getClass.getName)

  def spawn(config: ClockConfig): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(config),
    getClass.getSimpleName,
    MailboxSelector.fromConfig(MailboxConfig.UnboundedControlAwareMailbox),
    _
  )

  private def apply(config: ClockConfig): Behavior[Message] = Behaviors.setup: context =>
    Behaviors.withTimers: initialUpdateSpeedTimer =>
      context.system.receptionist ! Receptionist.subscribe(subscriberKey, context.self)
      val baseMachineTimestamp = Timestamp.machineNow()
      val baseTimestamp = config.start.initialTime match
        case Absolute(timestamp) => timestamp
        case Relative(duration)  => baseMachineTimestamp + duration
      val initialClockBase = config.start.condition match
        case Schedule(schedule) if schedule <= Duration(0, 0) =>
          ClockBase(baseMachineTimestamp, baseTimestamp, config.start.clockSpeed)

        case Schedule(schedule) =>
          initialUpdateSpeedTimer.startSingleTimer(
            SpeedUpdate(config.start.clockSpeed),
            schedule.secondsDouble.seconds
          )
          ClockBase(baseMachineTimestamp, baseTimestamp, 0)
      context.log.info(initialClockBase.toString)

      var clockBase = initialClockBase
      var subscribers = Set[ActorRef[ClockBase]]()
      var initialUpdateSpeedTimerFlag = true

      Behaviors.receiveMessage:
        case SpeedUpdate(clockSpeed) =>
          if initialUpdateSpeedTimerFlag then
            initialUpdateSpeedTimer.cancelAll()
            initialUpdateSpeedTimerFlag = false
          val baseMachineTimestamp = Timestamp.machineNow()
          clockBase = ClockBase(baseMachineTimestamp, clockBase.fromMachine(baseMachineTimestamp), clockSpeed)
          for subscriber <- subscribers do subscriber ! clockBase
          context.log.info(clockBase.toString)
          Behaviors.same

        case subscriberKey.Listing(newSubscribers) =>
          for subscriber <- newSubscribers &~ subscribers do subscriber ! clockBase
          subscribers = newSubscribers
          Behaviors.same

        case _: Receptionist.Listing =>
          Behaviors.unhandled
