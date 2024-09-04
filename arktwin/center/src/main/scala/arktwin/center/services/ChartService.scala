// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.actors.*
import arktwin.center.config.StaticCenterConfig
import arktwin.center.util.CenterKamon
import arktwin.center.util.CommonMessages.Terminate
import arktwin.common.GrpcHeaderKey
import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import com.google.protobuf.empty.Empty
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.apache.pekko.stream.{Attributes, Materializer, OverflowStrategy}
import org.apache.pekko.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

class ChartService(
    atlas: ActorRef[Atlas.Message],
    config: StaticCenterConfig,
    kamon: CenterKamon
)(using
    Materializer,
    ExecutionContext,
    Scheduler,
    Timeout
) extends ChartPowerApi:
  // micro-batch to reduce overhead of stream processing
  override def publish(in: Source[ChartPublishBatch, NotUsed], metadata: Metadata): Future[Empty] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.publish/$edgeId"

    val publishAgentNumCounter = kamon.chartPublishAgentNumCounter(edgeId)
    val publishBatchNumCounter = kamon.chartPublishBatchNumCounter(edgeId)
    val publishMachineLatencyHistogram = kamon.chartPublishMachineLatencyHistogram(edgeId)

    for
      chartRecorder <- atlas ? (Atlas.SpawnChartRecorder(
        edgeId,
        _: ActorRef[ActorRef[ChartRecorder.Message]]
      ))
      chart <- atlas ? (Atlas.SpawnChart(
        edgeId,
        _: ActorRef[ActorRef[Chart.Message]]
      ))
    do
      in
        .log(logName)
        .addAttributes(
          Attributes.logLevels(
            onFailure = Attributes.LogLevels.Warning,
            onFinish = Attributes.LogLevels.Warning
          )
        )
        .map: publishBatch =>
          val currentMachineTimestamp = Timestamp.machineNow()

          publishAgentNumCounter.increment(publishBatch.agents.size)
          publishBatchNumCounter.increment()
          publishMachineLatencyHistogram.record(
            Math.max(
              0,
              (currentMachineTimestamp - publishBatch.transmissionMachineTimestamp).millisLong
            ),
            publishBatch.agents.size
          )

          Chart.PublishBatch(publishBatch.agents, currentMachineTimestamp)
        .wireTap(ActorSink.actorRef(chartRecorder, Terminate, _ => Terminate))
        .to(ActorSink.actorRef(chart, Terminate, _ => Terminate))
        .run()
      scribe.info(s"[$logName] connected")
    Future.never

  // micro-batch to reduce overhead of stream processing
  override def subscribe(in: Empty, metadata: Metadata): Source[ChartSubscribeBatch, NotUsed] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.subscribe/$edgeId"

    val subscribeAgentNumCounter = kamon.chartSubscribeAgentNumCounter(edgeId)
    val subscribeBatchNumCounter = kamon.chartSubscribeBatchNumCounter(edgeId)
    val subscribeMachineLatencyHistogram = kamon.chartSubscribeMachineLatencyHistogram(edgeId)

    val (chartSubscriber, source) = ActorSource
      .actorRef[Chart.SubscribeBatch](
        PartialFunction.empty,
        PartialFunction.empty,
        config.subscribeBufferSize,
        OverflowStrategy.dropHead
      )
      .log(logName)
      .addAttributes(
        Attributes.logLevels(
          onFailure = Attributes.LogLevels.Warning,
          onFinish = Attributes.LogLevels.Warning
        )
      )
      .groupedWeightedWithin(config.subscribeBatchSize, config.subscribeBatchInterval)(
        _.agents.size
      )
      .map: subscribeBatch =>
        val currentMachineTimestamp = Timestamp.machineNow()

        for subscribe <- subscribeBatch do
          subscribeAgentNumCounter.increment(subscribe.agents.size)
          subscribeMachineLatencyHistogram.record(
            Math.max(
              0,
              (currentMachineTimestamp - subscribe.routeReceptionMachineTimestamp).millisLong
            ),
            subscribe.agents.size
          )
        subscribeBatchNumCounter.increment()

        ChartSubscribeBatch(subscribeBatch.flatMap(_.agents), currentMachineTimestamp)
      .preMaterialize()
    atlas ! Atlas.AddChartSubscriber(edgeId, chartSubscriber)
    scribe.info(s"spawned a chart subscriber for $edgeId: ${chartSubscriber.path}")
    scribe.info(s"[$logName] connected")
    source
