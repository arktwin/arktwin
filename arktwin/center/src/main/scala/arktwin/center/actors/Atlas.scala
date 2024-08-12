// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.DynamicCenterConfig.AtlasConfig
import arktwin.center.util.CenterKamon
import arktwin.common.MailboxConfig
import arktwin.common.data.Vector3Enu
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable

object Atlas:
  type Message = SpawnRecorder | RemoveRecorder | SpawnRouter | RemoveRouter | AddSubscriber | RemoveSubscriber |
    TimerReset.type
  case class SpawnRecorder(edgeId: String, replyTo: ActorRef[ActorRef[ChartRecorder.Message]])
  case class RemoveRecorder(edgeId: String)
  case class SpawnRouter(edgeId: String, replyTo: ActorRef[ActorRef[ChartRouter.Message]])
  case class RemoveRouter(edgeId: String)
  case class AddSubscriber(edgeId: String, subscriber: ActorRef[ChartRouter.SubscribeBatch])
  case class RemoveSubscriber(edgeId: String)
  object TimerReset

  case class PartitionIndex(x: Int, y: Int, z: Int):
    def neighbors: Seq[PartitionIndex] =
      for
        xd <- Seq(-1, 0, 1)
        yd <- Seq(-1, 0, 1)
        zd <- Seq(-1, 0, 1)
      yield PartitionIndex(x + xd, y + yd, z + zd)

  // TODO should depend on the config?
  case class ChartRecord(edgeId: String, config: AtlasConfig, indexes: Set[PartitionIndex])

  def spawn(
      config: AtlasConfig,
      kamon: CenterKamon
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(config, kamon),
    getClass.getSimpleName,
    MailboxConfig(getClass.getName),
    _
  )

  private def apply(
      config: AtlasConfig,
      kamon: CenterKamon
  ): Behavior[Message] = Behaviors.setup: context =>
    Behaviors.withTimers: timer =>
      timer.startSingleTimer(TimerReset, config.interval)

      var recorders = Map[String, ActorRef[ChartRecorder.Message]]()
      var routers = Map[String, ActorRef[ChartRouter.Message]]()
      var subscribers = Map[String, ActorRef[ChartRouter.SubscribeBatch]]()

      Behaviors.receiveMessage:
        case SpawnRecorder(edgeId, replyTo) =>
          val recorder = context.spawnAnonymous(
            ChartRecorder(edgeId, config),
            MailboxConfig(ChartRecorder.getClass.getName)
          )
          replyTo ! recorder
          context.watchWith(recorder, RemoveRecorder(edgeId))
          recorders += edgeId -> recorder
          context.log.info(s"spawned a recorder for $edgeId: ${recorder.path} ")
          Behaviors.same

        case RemoveRecorder(edgeId) =>
          recorders -= edgeId
          Behaviors.same

        case SpawnRouter(edgeId, replyTo) =>
          val initialRouteTable = new ChartRouter.RouteTable:
            override def apply(vector3: Vector3Enu): Seq[ActorRef[ChartRouter.SubscribeBatch]] =
              subscribers.filter(_._1 != edgeId).values.toSeq
          val router = context.spawnAnonymous(
            ChartRouter(edgeId, initialRouteTable, kamon),
            MailboxConfig(ChartRouter.getClass.getName)
          )
          replyTo ! router
          context.watchWith(router, RemoveRouter(edgeId))
          routers += edgeId -> router
          context.log.info(s"spawned a router for $edgeId: ${router.path}")
          Behaviors.same

        case RemoveRouter(edgeId) =>
          routers -= edgeId
          Behaviors.same

        case AddSubscriber(edgeId, subscriber) =>
          context.watchWith(subscriber, RemoveSubscriber(edgeId))
          subscribers += edgeId -> subscriber
          context.log.info(s"spawned a subscriber for $edgeId: ${subscriber.path}")
          Behaviors.same

        case RemoveSubscriber(edgeId) =>
          subscribers -= edgeId
          Behaviors.same

        case TimerReset =>
          timer.startSingleTimer(TimerReset, config.interval)
          context.spawnAnonymous(
            child(
              recorders,
              routers,
              subscribers,
              config
            )
          )
          Behaviors.same

  // TODO should time out?
  // TODO rename function instead of child
  private def child(
      recorders: Map[String, ActorRef[ChartRecorder.Message]],
      routers: Map[String, ActorRef[ChartRouter.Message]],
      subscribers: Map[String, ActorRef[ChartRouter.SubscribeBatch]],
      config: AtlasConfig
  ): Behavior[ChartRecord] = Behaviors.setup: context =>
    for (_, recorder) <- recorders do recorder ! ChartRecorder.Collect(config, context.self)
    val records = mutable.ArrayBuffer[ChartRecord]()
    Behaviors.receiveMessage:
      case record: ChartRecord =>
        records.addOne(record)
        if records.size >= recorders.size then
          config.culling match
            case AtlasConfig.Broadcast() =>
              for (edgeId, router) <- routers do
                router ! new ChartRouter.RouteTable:
                  override def apply(vector3: Vector3Enu): Seq[ActorRef[ChartRouter.SubscribeBatch]] =
                    subscribers.filter(_._1 != edgeId).values.toSeq

            case AtlasConfig.GridCulling(gridCellSize) =>
              val partitionToSender = records.toSeq
                .flatMap(a => a.indexes.flatMap(_.neighbors).map((_, a.edgeId)))
                .groupBy(_._1)
                .view
                .mapValues(_.flatMap(a => subscribers.get(a._2).map((a._2, _))))
                .toMap

              for (edgeId, router) <- routers do
                router ! new ChartRouter.RouteTable:
                  override def apply(vector3: Vector3Enu): Seq[ActorRef[ChartRouter.SubscribeBatch]] =
                    partitionToSender
                      .getOrElse(
                        PartitionIndex(
                          math.floor(vector3.x / gridCellSize.x).toInt,
                          math.floor(vector3.y / gridCellSize.y).toInt,
                          math.floor(vector3.z / gridCellSize.z).toInt
                        ),
                        Seq()
                      )
                      .filter(_._1 != edgeId)
                      .map(_._2)

              context.log.info(
                partitionToSender
                  .map((i, senders) => s"[${i.x},${i.y},${i.z}]->${senders.map(_._1).mkString("(", ",", ")")}")
                  .mkString("", ", ", "")
              )

          Behaviors.stopped
        else Behaviors.same
