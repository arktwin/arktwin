// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.StaticCenterConfig
import arktwin.center.actors.*
import arktwin.center.actors.CommonMessages.{Complete, Failure}
import arktwin.center.util.CenterKamon
import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.common.GrpcHeaderKey
import com.google.protobuf.empty.Empty
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.apache.pekko.stream.{Attributes, Materializer, OverflowStrategy}
import org.apache.pekko.util.Timeout
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

class ChartService(
    receptionist: ActorRef[Receptionist.Command],
    atlas: ActorRef[Atlas.Message],
    config: StaticCenterConfig,
    log: Logger,
    kamon: CenterKamon
)(using
    Materializer,
    ExecutionContext,
    Scheduler,
    Timeout
) extends ChartPowerApi:
  // micro-batch to reduce overhead of stream processing
  override def publish(in: Source[ChartAgentsPublish, NotUsed], metadata: Metadata): Future[Empty] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.publish/$edgeId"

    val publishAgentNumCounter = kamon.chartPublishAgentNumCounter(edgeId)
    val publishBatchNumCounter = kamon.chartPublishBatchNumCounter(edgeId)
    val publishMachineLatencyHistogram = kamon.chartPublishMachineLatencyHistogram(edgeId)

    val chartRecorderFuture = atlas ? (Atlas.SpawnRecorder(
      edgeId,
      _: ActorRef[ActorRef[ChartRecorder.Message]]
    ))
    val chartReceiverFuture = atlas ? (Atlas.SpawnReceiver(
      edgeId,
      _: ActorRef[ActorRef[ChartReceiver.Message]]
    ))
    for
      chartRecorder <- chartRecorderFuture
      chartReceiver <- chartReceiverFuture
    do
      in
        .log(logName)
        .addAttributes(
          Attributes.logLevels(onFailure = Attributes.LogLevels.Warning, onFinish = Attributes.LogLevels.Warning)
        )
        .map: publish =>
          val currentMachineTimestamp = Timestamp.machineNow()

          publishAgentNumCounter.increment(publish.agents.size)
          publishBatchNumCounter.increment()
          publishMachineLatencyHistogram.record(
            Math.max(0, (currentMachineTimestamp - publish.transmissionMachineTimestamp).millisLong),
            publish.agents.size
          )

          ChartReceiver.Publish(publish.agents, currentMachineTimestamp)
        .wireTap(ActorSink.actorRef(chartRecorder, Complete, a => Failure(a, edgeId)))
        .to(ActorSink.actorRef(chartReceiver, Complete, a => Failure(a, edgeId)))
        .run()
      log.info(s"[$logName] connected")
    Future.never

  // micro-batch to reduce overhead of stream processing
  override def subscribe(in: Empty, metadata: Metadata): Source[ChartAgentsSubscribe, NotUsed] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.subscribe/$edgeId"

    val subscribeAgentNumCounter = kamon.chartSubscribeAgentNumCounter(edgeId)
    val subscribeBatchNumCounter = kamon.chartSubscribeBatchNumCounter(edgeId)
    val subscribeMachineLatencyHistogram = kamon.chartSubscribeMachineLatencyHistogram(edgeId)

    val (subscriber, source) = ActorSource
      .actorRef[ChartSender.Subscribe](
        PartialFunction.empty,
        PartialFunction.empty,
        config.bufferSize,
        OverflowStrategy.dropHead
      )
      .log(logName)
      .addAttributes(
        Attributes.logLevels(onFailure = Attributes.LogLevels.Warning, onFinish = Attributes.LogLevels.Warning)
      )
      .groupedWeightedWithin(config.subscribeStreamBatchSize, config.subscribeStreamBatchInterval)(_.agents.size)
      .map: subscribes =>
        val currentMachineTimestamp = Timestamp.machineNow()

        for subscribe <- subscribes do
          subscribeAgentNumCounter.increment(subscribe.agents.size)
          subscribeMachineLatencyHistogram.record(
            Math.max(0, (currentMachineTimestamp - subscribe.forwardReceptionMachineTimestamp).millisLong),
            subscribe.agents.size
          )
        subscribeBatchNumCounter.increment()

        ChartAgentsSubscribe(subscribes.flatMap(_.agents), currentMachineTimestamp)
      .preMaterialize()
    atlas ! Atlas.SpawnSender(edgeId, subscriber)
    log.info(s"[$logName] connected")
    source
