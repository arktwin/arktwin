// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.actors.ChartRecorder.ChartRecord
import arktwin.center.configs.AtlasConfig
import arktwin.center.util.CenterKamon
import arktwin.common.util.BehaviorsExtensions.*
import arktwin.common.util.CommonMessages.Timeout
import arktwin.common.util.MailboxConfig
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable

object Atlas:
  type Message = SpawnChart | RemoveChart | SpawnChartRecorder | RemoveChartRecorder |
    AddChartSubscriber | RemoveChartSubscriber | SpawnUpdateRouteTable.type |
    UpdateRouteTableTerminated.type
  case class SpawnChart(edgeId: String, replyTo: ActorRef[ActorRef[Chart.Message]])
  case class RemoveChart(edgeId: String)
  case class SpawnChartRecorder(edgeId: String, replyTo: ActorRef[ActorRef[ChartRecorder.Message]])
  case class RemoveChartRecorder(edgeId: String)
  case class AddChartSubscriber(edgeId: String, subscriber: ActorRef[Chart.SubscribeBatch])
  case class RemoveChartSubscriber(edgeId: String)
  object SpawnUpdateRouteTable
  object UpdateRouteTableTerminated

  case class PartitionIndex(x: Int, y: Int, z: Int):
    def neighbors: Seq[PartitionIndex] =
      for
        xd <- Seq(-1, 0, 1)
        yd <- Seq(-1, 0, 1)
        zd <- Seq(-1, 0, 1)
      yield PartitionIndex(x + xd, y + yd, z + zd)

  def spawn(
      config: AtlasConfig,
      kamon: CenterKamon
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(config, kamon),
    getClass.getSimpleName,
    MailboxConfig(this),
    _
  )

  def apply(
      config: AtlasConfig,
      kamon: CenterKamon
  ): Behavior[Message] = Behaviors.setupWithLogger: (context, logger) =>
    Behaviors.withTimers: timer =>
      timer.startSingleTimer(SpawnUpdateRouteTable, config.routeTableUpdateInterval)

      var chartRecorders = Map[String, ActorRef[ChartRecorder.Message]]()
      var charts = Map[String, ActorRef[Chart.Message]]()
      var chartSubscribers = Map[String, ActorRef[Chart.SubscribeBatch]]()

      Behaviors.receiveMessage:
        case SpawnChart(edgeId, replyTo) =>
          val initialRouteTable: Chart.RouteTable = config.culling match
            case AtlasConfig.Broadcast()               => _ => chartSubscribers
            case AtlasConfig.GridCulling(gridCellSize) => _ => Map()
          val chart = context.spawnAnonymous(
            Chart(edgeId, initialRouteTable, kamon),
            MailboxConfig(Chart)
          )
          replyTo ! chart
          context.watchWith(chart, RemoveChart(edgeId))
          charts += edgeId -> chart
          logger.info(s"spawned a chart for $edgeId: ${chart.path}")
          Behaviors.same

        case RemoveChart(edgeId) =>
          charts -= edgeId
          Behaviors.same

        case SpawnChartRecorder(edgeId, replyTo) =>
          val chartRecorder = context.spawnAnonymous(
            ChartRecorder(edgeId),
            MailboxConfig(ChartRecorder)
          )
          replyTo ! chartRecorder
          context.watchWith(chartRecorder, RemoveChartRecorder(edgeId))
          chartRecorders += edgeId -> chartRecorder
          logger.info(s"spawned a chart recorder for $edgeId: ${chartRecorder.path} ")
          Behaviors.same

        case RemoveChartRecorder(edgeId) =>
          chartRecorders -= edgeId
          Behaviors.same

        case AddChartSubscriber(edgeId, chartSubscriber) =>
          context.watchWith(chartSubscriber, RemoveChartSubscriber(edgeId))
          chartSubscribers += edgeId -> chartSubscriber
          Behaviors.same

        case RemoveChartSubscriber(edgeId) =>
          chartSubscribers -= edgeId
          Behaviors.same

        case UpdateRouteTableTerminated =>
          timer.startSingleTimer(SpawnUpdateRouteTable, config.routeTableUpdateInterval)
          Behaviors.same

        case SpawnUpdateRouteTable =>
          val session = context.spawnAnonymous(
            updateRouteTable(
              chartRecorders,
              charts,
              chartSubscribers,
              config
            )
          )
          context.watchWith(session, UpdateRouteTableTerminated)
          Behaviors.same

  private def updateRouteTable(
      chartRecorders: Map[String, ActorRef[ChartRecorder.Message]],
      charts: Map[String, ActorRef[Chart.Message]],
      chartSubscribers: Map[String, ActorRef[Chart.SubscribeBatch]],
      config: AtlasConfig
  ): Behavior[ChartRecord | Timeout.type] = Behaviors.setupWithLogger: (context, logger) =>
    Behaviors.withTimers: timer =>
      config.culling match
        case AtlasConfig.Broadcast() =>
          val updateRouteTable = Chart.UpdateRouteTable(_ => chartSubscribers)
          for (_, chart) <- charts do chart ! updateRouteTable
          Behaviors.stopped

        case AtlasConfig.GridCulling(gridCellSize) =>
          timer.startSingleTimer(Timeout, config.routeTableUpdateInterval)

          for (_, chartRecorder) <- chartRecorders do
            chartRecorder ! ChartRecorder.Get(config, context.self)
          val records = mutable.ArrayBuffer[ChartRecord]()

          Behaviors.receiveMessage:
            case record: ChartRecord if records.size + 1 < chartRecorders.size =>
              records.addOne(record)
              Behaviors.same

            case record: ChartRecord =>
              records.addOne(record)
              val partitionToSubscriber =
                records
                  .flatMap(record =>
                    chartSubscribers
                      .get(record.edgeId)
                      .toSeq
                      .flatMap(chartSubscriber =>
                        record.indexes
                          .flatMap(_.neighbors)
                          .toSet
                          .map((_, (record.edgeId, chartSubscriber)))
                      )
                  )
                  .groupMap(_._1)(_._2)
                  .view
                  .mapValues(_.toMap)
                  .toMap

              val updateRouteTable = Chart.UpdateRouteTable(vector3 =>
                partitionToSubscriber
                  .getOrElse(
                    PartitionIndex(
                      math.floor(vector3.x / gridCellSize.x).toInt,
                      math.floor(vector3.y / gridCellSize.y).toInt,
                      math.floor(vector3.z / gridCellSize.z).toInt
                    ),
                    Map()
                  )
              )
              for (_, chart) <- charts do chart ! updateRouteTable

              logger.debug(
                partitionToSubscriber.toSeq
                  .sortBy((index, _) => (index.x, index.y, index.z))
                  .map((i, senders) =>
                    s"[${i.x},${i.y},${i.z}]->${senders.map(_._1).mkString("(", ",", ")")}"
                  )
                  .mkString("", ", ", "")
              )

              Behaviors.stopped

            case Timeout =>
              Behaviors.stopped
