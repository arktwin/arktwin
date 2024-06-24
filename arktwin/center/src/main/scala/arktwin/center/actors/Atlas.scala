/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.actors

import arktwin.center.DynamicCenterConfig.AtlasConfig
import arktwin.common.MailboxConfig
import arktwin.common.data.Vector3Enu
import org.apache.pekko.actor.typed.SpawnProtocol.Spawn
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Behavior, MailboxSelector}

import scala.collection.mutable

object Atlas:
  type Message = SpawnRecorder | SpawnReceiver | SpawnSender | Reset.type
  case class SpawnRecorder(edgeId: String, replyTo: ActorRef[ActorRef[ChartRecorder.Message]])
  case class SpawnReceiver(edgeId: String, replyTo: ActorRef[ActorRef[ChartReceiver.Message]])
  case class SpawnSender(edgeId: String, subscriber: ActorRef[ChartSender.Subscribe])
  object Reset

  case class PartitionIndex(x: Int, y: Int, z: Int) {
    def neighbors: Seq[PartitionIndex] =
      for
        xd <- Seq(-1, 0, 1)
        yd <- Seq(-1, 0, 1)
        zd <- Seq(-1, 0, 1)
      yield PartitionIndex(x + xd, y + yd, z + zd)
  }
  // TODO should depend on the config?
  case class ChartRecord(edgeId: String, config: AtlasConfig, indexes: Set[PartitionIndex])

  def spawn(
      config: AtlasConfig
  ): ActorRef[ActorRef[Message]] => Spawn[Message] = Spawn(
    apply(config),
    getClass.getSimpleName,
    MailboxSelector.fromConfig(MailboxConfig.UnboundedControlAwareMailbox),
    _
  )

  private def apply(
      config: AtlasConfig
  ): Behavior[Message] =
    Behaviors.setup: context =>
      Behaviors.withTimers: timer =>
        timer.startSingleTimer(Reset, config.interval)

        val chartRecorderParent = context.spawn(
          ChartRecorderParent(),
          ChartRecorderParent.getClass.getSimpleName
        )
        val chartReceiverParent = context.spawn(
          ChartReceiverParent(),
          ChartReceiverParent.getClass.getSimpleName
        )
        val chartSenderParent = context.spawn(
          ChartSenderParent(),
          ChartSenderParent.getClass.getSimpleName
        )

        Behaviors.receiveMessage:
          case SpawnRecorder(edgeId, replyTo) =>
            chartRecorderParent ! ChartRecorderParent.SpawnRecorder(edgeId, config, replyTo)
            Behaviors.same

          case SpawnReceiver(edgeId, replyTo) =>
            context.spawnAnonymous(
              receiverSpawner(
                chartSenderParent,
                chartReceiverParent,
                edgeId,
                replyTo
              )
            )
            Behaviors.same

          case SpawnSender(edgeId, subscriber) =>
            chartSenderParent ! ChartSenderParent.SpawnSender(edgeId, subscriber)
            Behaviors.same

          case Reset =>
            timer.startSingleTimer(Reset, config.interval)
            context.spawnAnonymous(
              child(
                chartRecorderParent,
                chartReceiverParent,
                chartSenderParent,
                config
              )
            )
            Behaviors.same

  private type ReceiverSpawner = ChartSenderParent.ReadSendersReply

  private def receiverSpawner(
      chartSenderParent: ActorRef[ChartSenderParent.Message],
      chartReceiverParent: ActorRef[ChartReceiverParent.Message],
      edgeId: String,
      replyTo: ActorRef[ActorRef[ChartReceiver.Message]]
  ): Behavior[ReceiverSpawner] = Behaviors.setup: context =>
    chartSenderParent ! ChartSenderParent.ReadSenders(context.self)
    Behaviors.receiveMessage:
      case ChartSenderParent.ReadSendersReply(senders) =>
        val initialForwarder = new ChartReceiver.Forwarder:
          override def apply(vector3: Vector3Enu): Seq[ActorRef[ChartSender.Message]] =
            senders.filter(_._1 != edgeId).values.toSeq
        chartReceiverParent ! ChartReceiverParent.SpawnReceiver(edgeId, initialForwarder, replyTo)
        Behaviors.stopped

  private type ChildMessage = ChartRecorderParent.ReadRecordersReply | ChartReceiverParent.ReadReceiversReply |
    ChartSenderParent.ReadSendersReply | ChartRecord

  // TODO should time out?
  // TODO rename function instead of child
  private def child(
      chartRecorderParent: ActorRef[ChartRecorderParent.Message],
      chartReceiverParent: ActorRef[ChartReceiverParent.Message],
      chartSenderParent: ActorRef[ChartSenderParent.Message],
      config: AtlasConfig
  ): Behavior[ChildMessage] = Behaviors.setup: context =>
    chartRecorderParent ! ChartRecorderParent.ReadRecorders(context.self)
    var recordersWait = Option.empty[Map[String, ActorRef[ChartRecorder.Message]]]

    chartReceiverParent ! ChartReceiverParent.ReadReceivers(context.self)
    var receiversWait = Option.empty[Map[String, ActorRef[ChartReceiver.Message]]]

    chartSenderParent ! ChartSenderParent.ReadSenders(context.self)
    var sendersWait = Option.empty[Map[String, ActorRef[ChartSender.Message]]]

    def next(): Behavior[ChildMessage] = (recordersWait, receiversWait, sendersWait) match
      case (Some(recorders), Some(receivers), Some(senders)) =>
        for (_, recorder) <- recorders do recorder ! ChartRecorder.Collect(config, context.self)
        val records = mutable.ArrayBuffer[ChartRecord]()
        Behaviors.receiveMessage:
          case record: ChartRecord =>
            records.addOne(record)
            if records.size >= recorders.size then
              config.culling match
                case AtlasConfig.Broadcast() =>
                  for (edgeId, receiver) <- receivers do
                    receiver ! new ChartReceiver.Forwarder:
                      override def apply(vector3: Vector3Enu): Seq[ActorRef[ChartSender.Message]] =
                        senders.filter(_._1 != edgeId).values.toSeq

                case AtlasConfig.GridCulling(gridCellSize) =>
                  val partitionToSender = records.toSeq
                    .flatMap(a => a.indexes.flatMap(_.neighbors).map((_, a.edgeId)))
                    .groupBy(_._1)
                    .view
                    .mapValues(_.flatMap(a => senders.get(a._2).map((a._2, _))))
                    .toMap

                  for (edgeId, receiver) <- receivers do
                    receiver ! new ChartReceiver.Forwarder:
                      override def apply(vector3: Vector3Enu): Seq[ActorRef[ChartSender.Message]] =
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

          case _ =>
            Behaviors.unhandled

      case _ =>
        Behaviors.same

    Behaviors.receiveMessage:
      case ChartRecorderParent.ReadRecordersReply(recorders) =>
        recordersWait = Some(recorders)
        next()

      case ChartReceiverParent.ReadReceiversReply(receivers) =>
        receiversWait = Some(receivers)
        next()

      case ChartSenderParent.ReadSendersReply(senders) =>
        sendersWait = Some(senders)
        next()

      case _ =>
        Behaviors.unhandled
