// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.connectors

import arktwin.center.services.{ChartAgent, ChartAgentsPublish, ChartClient}
import arktwin.common.GrpcHeaderKey
import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.actors.sinks.Chart
import arktwin.edge.StaticEdgeConfig
import arktwin.edge.util.CommonMessages.Nop
import arktwin.edge.util.EdgeKamon
import com.google.protobuf.empty.Empty
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}

object ChartConnector:
  case class Publish(agents: Seq[ChartAgent], putReceptionMachineTimestamp: Timestamp)

// TODO retry connection in actor?
case class ChartConnector(client: ChartClient, staticConfig: StaticEdgeConfig, edgeId: String, kamon: EdgeKamon)(using
    Materializer
):
  import ChartConnector.*

  private val publishAgentNumCounter = kamon.chartPublishAgentNumCounter()
  private val publishBatchNumCounter = kamon.chartPublishBatchNumCounter()
  private val publishMachineLatencyHistogram = kamon.chartPublishMachineLatencyHistogram()

  def publish(): ActorRef[Publish] =
    // micro-batch to reduce overhead of stream processing
    val (actorRef, source) = ActorSource
      .actorRef[Publish](
        PartialFunction.empty,
        PartialFunction.empty,
        staticConfig.publishBufferSize,
        OverflowStrategy.dropHead
      )
      .mapConcat: a =>
        val currentMachineTimestamp = Timestamp.machineNow()
        val batches = a.agents
          .grouped(staticConfig.publishBatchSize)
          .map(agents => ChartAgentsPublish(agents, currentMachineTimestamp))
          .toSeq
        publishAgentNumCounter.increment(a.agents.size)
        publishBatchNumCounter.increment(batches.size)
        publishMachineLatencyHistogram.record(
          Math.max(0, (currentMachineTimestamp - a.putReceptionMachineTimestamp).millisLong),
          a.agents.size
        )
        batches
      .preMaterialize()
    client
      .publish()
      .addHeader(GrpcHeaderKey.edgeId, edgeId)
      .invoke(source)
    actorRef

  private val subscribeAgentNumCounter = kamon.chartSubscribeAgentNumCounter()
  private val subscribeMachineLatencyHistogram = kamon.chartSubscribeMachineLatencyHistogram()

  def subscribe(chart: ActorRef[Chart.Message]): Unit =
    // TODO maicro-batch?
    // send agents information to Chart individually for quick response edge/neighbors/_query
    client
      .subscribe()
      .addHeader(GrpcHeaderKey.edgeId, edgeId)
      .invoke(Empty())
      .log(getClass.getSimpleName + ".subscribe")
      .mapConcat: a =>
        val currentMachineTimestamp = Timestamp.machineNow()
        subscribeAgentNumCounter.increment(a.agents.size)
        subscribeMachineLatencyHistogram.record(
          Math.max(0, (currentMachineTimestamp - a.transmissionMachineTimestamp).millisLong),
          a.agents.size
        )
        a.agents.map(Chart.Catch.apply)
      .to(ActorSink.actorRef(chart, Nop, _ => Nop))
      .run()
