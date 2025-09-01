// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.configs.ClockConfig
import arktwin.center.configs.ClockConfig.Start.{Absolute, Relative, Schedule}
import arktwin.center.services.ClockBase
import arktwin.center.services.ClockBaseExtensions.*
import arktwin.common.data.TimestampExtensions.*
import arktwin.common.data.{TaggedDuration, TaggedTimestamp}
import arktwin.common.util.BehaviorsExtensions.*
import arktwin.common.util.{MachineTag, MailboxConfig}
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.DurationDouble

object Clock:
  type Message = AddSubscriber | Read | RemoveSubscriber | UpdateSpeed
  case class AddSubscriber(edgeId: String, subscriber: ActorRef[ClockBase])
  case class Read(replyTo: ActorRef[ClockBase])
  case class RemoveSubscriber(edgeId: String)
  case class UpdateSpeed(clockSpeed: Double)

  def spawn(config: ClockConfig): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(config),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      config: ClockConfig
  ): Behavior[Message] = Behaviors.setupWithLogger: (context, logger) =>
    Behaviors.withTimers: initialUpdateSpeedTimer =>
      val baseMachineTimestamp = TaggedTimestamp.machineNow()
      val baseVirtualTimestamp = config.start.initialTime match
        case Absolute(timestamp) => timestamp
        case Relative(duration)  => baseMachineTimestamp.untag.tagVirtual + duration
      val initialClockBase = config.start.condition match
        case Schedule(schedule) if schedule <= TaggedDuration[MachineTag](0, 0) =>
          ClockBase(baseMachineTimestamp, baseVirtualTimestamp, config.start.clockSpeed)

        case Schedule(schedule) =>
          initialUpdateSpeedTimer.startSingleTimer(
            UpdateSpeed(config.start.clockSpeed),
            schedule.secondsDouble.seconds
          )
          ClockBase(baseMachineTimestamp, baseVirtualTimestamp, 0)

      var clockBase = initialClockBase
      logger.info(clockBase.toString)

      var subscribers = Map[String, ActorRef[ClockBase]]()
      var initialUpdateSpeedTimerFlag = true

      Behaviors.receiveMessage:
        case AddSubscriber(edgeId, subscriber) =>
          subscriber ! clockBase
          context.watchWith(subscriber, RemoveSubscriber(edgeId))
          subscribers += edgeId -> subscriber
          Behaviors.same

        case Read(replyTo) =>
          replyTo ! clockBase
          Behaviors.same

        case RemoveSubscriber(edgeId) =>
          subscribers -= edgeId
          Behaviors.same

        case UpdateSpeed(clockSpeed) =>
          if initialUpdateSpeedTimerFlag then
            initialUpdateSpeedTimer.cancelAll()
            initialUpdateSpeedTimerFlag = false
          val baseMachineTimestamp = TaggedTimestamp.machineNow()
          clockBase = ClockBase(
            baseMachineTimestamp,
            clockBase.fromMachine(baseMachineTimestamp),
            clockSpeed
          )
          for subscriber <- subscribers.values do subscriber ! clockBase
          logger.info(clockBase.toString)
          Behaviors.same
