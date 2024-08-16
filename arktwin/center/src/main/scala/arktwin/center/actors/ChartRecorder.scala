// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.AtlasConfig
import arktwin.center.actors.Atlas.PartitionIndex
import arktwin.center.util.CommonMessages.Terminate
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable

object ChartRecorder:
  type Message = ChartRouter.PublishBatch | Collect | Terminate.type
  case class Collect(
      config: AtlasConfig,
      replyTo: ActorRef[ChartRecord]
  )

  // TODO should depend on the config?
  case class ChartRecord(edgeId: String, config: AtlasConfig, indexes: Set[PartitionIndex])

  def apply(edgeId: String, initialConfig: AtlasConfig): Behavior[Message] =
    var config = initialConfig
    var indexes = mutable.Set[PartitionIndex]()

    Behaviors.receiveMessage:
      case publishBatch: ChartRouter.PublishBatch =>
        // TODO consider relative coordinates

        config.culling match
          case AtlasConfig.Broadcast() =>

          case AtlasConfig.GridCulling(gridCellSize) =>
            for agent <- publishBatch.agents do
              indexes.add(
                PartitionIndex(
                  math.floor(agent.transform.localTranslationMeter.x / gridCellSize.x).toInt,
                  math.floor(agent.transform.localTranslationMeter.y / gridCellSize.y).toInt,
                  math.floor(agent.transform.localTranslationMeter.z / gridCellSize.z).toInt
                )
              )
        Behaviors.same

      case Collect(newConfig, replyTo) =>
        replyTo ! ChartRecord(edgeId, config, indexes.toSet)
        config = newConfig
        indexes = mutable.Set()
        Behaviors.same

      case Terminate =>
        Behaviors.stopped
