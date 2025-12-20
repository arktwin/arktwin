// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.actors.Atlas.PartitionIndex
import arktwin.center.configs.AtlasConfig
import arktwin.center.services.ChartAgent
import arktwin.common.data.Vector3Enu
import arktwin.common.util.CommonMessages.Terminate
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable

object ChartRecorder:
  type Message = Chart.PublishBatch | Get | Terminate.type
  case class Get(
      config: AtlasConfig,
      replyTo: ActorRef[ChartRecord]
  )

  case class ChartRecord(
      edgeId: String,
      cellAgentCounts: Map[Vector3Enu, Map[PartitionIndex, Int]]
  )

  def apply(
      edgeId: String
  ): Behavior[Message] =
    // TODO expire agents
    val agents = mutable.Map[String, ChartAgent]()

    Behaviors.receiveMessage:
      // TODO consider relative coordinates
      case publishBatch: Chart.PublishBatch =>
        for
          newAgent <- publishBatch.agents
          if agents
            .get(newAgent.agentId)
            .forall(
              _.transform.timestamp <= newAgent.transform.timestamp
            )
        do agents(newAgent.agentId) = newAgent
        Behaviors.same

      case Get(config, replyTo) =>
        config.culling match
          case AtlasConfig.Broadcast() =>

          case AtlasConfig.GridCulling(gridCellSizes, _) =>
            val cellAgentCounts = gridCellSizes
              .map(gridCellSize =>
                gridCellSize -> agents.values
                  .map(agent =>
                    PartitionIndex(
                      math
                        .floor(agent.transform.localTranslationMeter.x / gridCellSize.x)
                        .toInt,
                      math
                        .floor(agent.transform.localTranslationMeter.y / gridCellSize.y)
                        .toInt,
                      math
                        .floor(agent.transform.localTranslationMeter.z / gridCellSize.z)
                        .toInt
                    )
                  )
                  .groupMapReduce(identity)(_ => 1)(_ + _)
              )
              .toMap
            replyTo ! ChartRecord(edgeId, cellAgentCounts)
        Behaviors.same

      case Terminate =>
        Behaviors.stopped
