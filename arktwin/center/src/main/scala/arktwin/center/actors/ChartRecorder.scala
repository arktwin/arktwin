// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.DynamicCenterConfig.AtlasConfig
import arktwin.center.actors.Atlas.{ChartRecord, PartitionIndex}
import arktwin.center.util.CommonMessages.{Complete, Failure}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable

object ChartRecorder:
  type Message = ChartReceiver.Publish | Collect | Complete.type | Failure
  case class Collect(
      config: AtlasConfig,
      replyTo: ActorRef[ChartRecord]
  )

  def apply(edgeId: String, initialConfig: AtlasConfig): Behavior[Message] =
    var config = initialConfig
    var indexes = mutable.Set[PartitionIndex]()

    Behaviors.receiveMessage:
      case publish: ChartReceiver.Publish =>
        // TODO consider relative coordinates

        config.culling match
          case AtlasConfig.Broadcast() =>

          case AtlasConfig.GridCulling(gridCellSize) =>
            for agent <- publish.agents do
              indexes.add(
                PartitionIndex(
                  math.floor(agent.transform.localTranslationMeter.x / gridCellSize.x).toInt,
                  math.floor(agent.transform.localTranslationMeter.y / gridCellSize.y).toInt,
                  math.floor(agent.transform.localTranslationMeter.z / gridCellSize.z).toInt
                )
              )
        Behaviors.same

      case Collect(newConfig, replyTo) =>
        replyTo ! Atlas.ChartRecord(edgeId, config, indexes.toSet)
        config = newConfig
        indexes = mutable.Set()
        Behaviors.same

      case Complete =>
        Behaviors.stopped

      case Failure(throwable, edgeId) =>
        Behaviors.stopped
