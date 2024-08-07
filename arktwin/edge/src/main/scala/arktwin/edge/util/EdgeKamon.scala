// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import kamon.Kamon
import kamon.metric.Counter
import kamon.metric.Histogram
import kamon.tag.TagSet

object EdgeKamon:
  val runIdKey: String = "run_id"
  val edgeIdKey: String = "edge_id"
  val endpointKey: String = "endpoint"
  val recipientKey: String = "recipient"

  val chartPublishAgentNumName: String = "arktwin_edge_chart_1_publish_agent_num"
  val chartPublishBatchNumName: String = "arktwin_edge_chart_1_publish_batch_num"
  val chartPublishMachineLatencyName: String = "arktwin_edge_chart_1_publish_from_put_machine_latency"
  val chartSubscribeAgentNumName: String = "arktwin_edge_chart_5_subscribe_agent_num"
  val chartSubscribeMachineLatencyName: String = "arktwin_edge_chart_5_subscribe_from_center_machine_latency"
  val deadLetterNumName: String = "arktwin_edge_dead_letter_num"
  val restRequestNumName: String = "arktwin_edge_rest_request_num"
  val restAgentNumName: String = "arktwin_edge_rest_agent_num"
  val restProcessMachineTimeName: String = "arktwin_edge_rest_process_machine_time"
  val restSimulationLatencyName: String = "arktwin_edge_rest_simulation_latency"

class EdgeKamon(runId: String, edgeId: String):
  import EdgeKamon.*
  val commonTags = TagSet.of(runIdKey, runId).withTag(edgeIdKey, edgeId)

  def chartPublishAgentNumCounter(): Counter = Kamon
    .counter(chartPublishAgentNumName)
    .withTags(commonTags)

  def chartPublishBatchNumCounter(): Counter = Kamon
    .counter(chartPublishBatchNumName)
    .withTags(commonTags)

  def chartPublishMachineLatencyHistogram(): Histogram = Kamon
    .histogram(chartPublishMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTags(commonTags)

  def chartSubscribeAgentNumCounter(): Counter = Kamon
    .counter(chartSubscribeAgentNumName)
    .withTags(commonTags)

  def chartSubscribeMachineLatencyHistogram(): Histogram = Kamon
    .histogram(chartSubscribeMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTags(commonTags)

  def deadLetterNumCounter(): Counter = Kamon
    .counter(deadLetterNumName)
    .withTags(commonTags)

  def restRequestNumCounter(endpoint: String): Counter = Kamon
    .counter(restRequestNumName)
    .withTags(commonTags.withTag(endpointKey, endpoint))

  def restAgentNumCounter(endpoint: String): Counter = Kamon
    .counter(restAgentNumName)
    .withTags(commonTags.withTag(endpointKey, endpoint))

  def restProcessMachineTimeHistogram(endpoint: String): Histogram = Kamon
    .histogram(restProcessMachineTimeName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTags(commonTags.withTag(endpointKey, endpoint))

  // TODO how to handle negative numbers?
  def restSimulationLatencyHistogram(endpoint: String): Histogram = Kamon
    .histogram(restSimulationLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTags(commonTags.withTag(endpointKey, endpoint))
