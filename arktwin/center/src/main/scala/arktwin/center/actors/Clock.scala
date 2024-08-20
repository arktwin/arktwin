// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.ClockConfig
import arktwin.center.ClockConfig.Start.{Absolute, Relative, Schedule}
import arktwin.center.services.ClockBase
import arktwin.center.services.ClockBaseEx.*
import arktwin.common.MailboxConfig
import arktwin.common.data.DurationEx.given
import arktwin.common.data.TimestampEx.*
import arktwin.common.data.{Duration, Timestamp}
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.DurationDouble
import scala.math.Ordered.orderingToOrdered

object Clock:
  type Message = UpdateSpeed | AddSubscriber | RemoveSubscriber
  case class UpdateSpeed(clockSpeed: Double)
  case class AddSubscriber(edgeId: String, subscriber: ActorRef[ClockBase])
  case class RemoveSubscriber(edgeId: String)

  def spawn(config: ClockConfig): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(config),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  private def apply(config: ClockConfig): Behavior[Message] = Behaviors.setup: context =>
    Behaviors.withTimers: initialUpdateSpeedTimer =>
      val baseMachineTimestamp = Timestamp.machineNow()
      val baseTimestamp = config.start.initialTime match
        case Absolute(timestamp) => timestamp
        case Relative(duration)  => baseMachineTimestamp + duration
      val initialClockBase = config.start.condition match
        case Schedule(schedule) if schedule <= Duration(0, 0) =>
          ClockBase(baseMachineTimestamp, baseTimestamp, config.start.clockSpeed)

        case Schedule(schedule) =>
          initialUpdateSpeedTimer.startSingleTimer(
            UpdateSpeed(config.start.clockSpeed),
            schedule.secondsDouble.seconds
          )
          ClockBase(baseMachineTimestamp, baseTimestamp, 0)
      context.log.info(initialClockBase.toString)

      var clockBase = initialClockBase
      var subscribers = Map[String, ActorRef[ClockBase]]()
      var initialUpdateSpeedTimerFlag = true

      Behaviors.receiveMessage:
        case UpdateSpeed(clockSpeed) =>
          if initialUpdateSpeedTimerFlag then
            initialUpdateSpeedTimer.cancelAll()
            initialUpdateSpeedTimerFlag = false
          val baseMachineTimestamp = Timestamp.machineNow()
          clockBase =
            ClockBase(baseMachineTimestamp, clockBase.fromMachine(baseMachineTimestamp), clockSpeed)
          for subscriber <- subscribers.values do subscriber ! clockBase
          context.log.info(clockBase.toString)
          Behaviors.same

        case AddSubscriber(edgeId, subscriber) =>
          subscriber ! clockBase
          context.watchWith(subscriber, RemoveSubscriber(edgeId))
          subscribers += edgeId -> subscriber
          Behaviors.same

        case RemoveSubscriber(edgeId) =>
          subscribers -= edgeId
          Behaviors.same
