/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.actors

import arktwin.center.actors.CommonMessages.{Complete, Failure}
import arktwin.center.services.ChartAgent
import arktwin.center.util.CenterKamon
import arktwin.common.data.{Timestamp, Vector3Enu}
import arktwin.common.data.DurationEx.*
import arktwin.common.data.TimestampEx.*
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object ChartReceiver:
  type Message = Forwarder | Publish | Complete.type | Failure
  case class Publish(agents: Seq[ChartAgent], publishReceptionMachineTimestamp: Timestamp)

  trait Forwarder:
    def apply(vector3: Vector3Enu): Seq[ActorRef[ChartSender.Message]]

  def apply(edgeId: String, initialForwarder: Forwarder): Behavior[Message] =
    var forwarder = initialForwarder
    val forwardAgentNumCounter = CenterKamon.chartForwardAgentNumCounter(edgeId)
    val forwardBatchNumCounter = CenterKamon.chartForwardBatchNumCounter(edgeId)
    val forwardMachineLatencyHistogram = CenterKamon.chartForwardMachineLatencyHistogram(edgeId)

    Behaviors.receiveMessage:
      case newForwarder: Forwarder =>
        forwarder = newForwarder
        Behaviors.same

      // micro-batch to reduce overhead of stream processing
      case publish: Publish =>
        val currentMachineTimestamp = Timestamp.machineNow()

        // TODO consider relative coordinates
        for (sender, agents) <- publish.agents
            .flatMap(agent => forwarder(agent.transform.localTranslationMeter).map(sender => (agent, sender)))
            .groupMap(_._2)(_._1)
        do
          sender ! ChartSender.Subscribe(agents, currentMachineTimestamp)
          forwardAgentNumCounter.increment(agents.size)
          forwardBatchNumCounter.increment()

        forwardMachineLatencyHistogram.record(
          Math.max(0, (currentMachineTimestamp - publish.publishReceptionMachineTimestamp).millisLong),
          publish.agents.size
        )

        Behaviors.same

      case Complete =>
        Behaviors.stopped

      case Failure(throwable, edgeId) =>
        Behaviors.stopped
