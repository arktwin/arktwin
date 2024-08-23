// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.services.ChartAgent
import arktwin.center.util.CenterKamon
import arktwin.center.util.CommonMessages.Terminate
import arktwin.common.data.DurationEx.*
import arktwin.common.data.TimestampEx.*
import arktwin.common.data.{Timestamp, Vector3Enu}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

object Chart:
  type Message = PublishBatch | UpdateRouteTable | Terminate.type
  case class PublishBatch(agents: Seq[ChartAgent], publishReceptionMachineTimestamp: Timestamp)
  case class UpdateRouteTable(routeTable: RouteTable)

  case class SubscribeBatch(agents: Seq[ChartAgent], routeReceptionMachineTimestamp: Timestamp)

  trait RouteTable:
    def apply(vector3: Vector3Enu): Map[String, ActorRef[SubscribeBatch]]

  def apply(edgeId: String, initialRouteTable: RouteTable, kamon: CenterKamon): Behavior[Message] =
    var routeTable = initialRouteTable
    val routeAgentNumCounter = kamon.chartRouteAgentNumCounter(edgeId)
    val routeBatchNumCounter = kamon.chartRouteBatchNumCounter(edgeId)
    val routeMachineLatencyHistogram = kamon.chartRouteMachineLatencyHistogram(edgeId)

    Behaviors.receiveMessage:
      // micro-batch to reduce overhead of stream processing
      case publishBatch: PublishBatch =>
        val currentMachineTimestamp = Timestamp.machineNow()

        // TODO consider relative coordinates
        for (chartSubscriber, agents) <- publishBatch.agents
            .flatMap(agent =>
              routeTable(agent.transform.localTranslationMeter)
                .removed(edgeId)
                .values
                .map((_, agent))
            )
            .groupMap(_._1)(_._2)
        do
          chartSubscriber ! SubscribeBatch(agents, currentMachineTimestamp)
          routeAgentNumCounter.increment(agents.size)
          routeBatchNumCounter.increment()

        routeMachineLatencyHistogram.record(
          Math.max(
            0,
            (currentMachineTimestamp - publishBatch.publishReceptionMachineTimestamp).millisLong
          ),
          publishBatch.agents.size
        )

        Behaviors.same

      case updateRouteTable: UpdateRouteTable =>
        routeTable = updateRouteTable.routeTable
        Behaviors.same

      case Terminate =>
        Behaviors.stopped