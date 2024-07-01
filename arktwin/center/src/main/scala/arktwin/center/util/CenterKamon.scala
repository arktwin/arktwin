// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.util

import kamon.Kamon
import kamon.metric.Counter
import kamon.metric.Histogram
import kamon.tag.TagSet

object CenterKamon:
  val runIdKey: String = "run_id"
  val edgeIdKey: String = "edge_id"

class CenterKamon(runId: String):
  import CenterKamon.*
  val commonTags = TagSet.of(runIdKey, runId)

  val chartPublishAgentNumName: String = "arktwin_center_chart_2_publish_agent_num"
  def chartPublishAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishAgentNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartPublishBatchNumName: String = "arktwin_center_chart_2_publish_batch_num"
  def chartPublishBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishBatchNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartPublishMachineLatencyName: String = "arktwin_center_chart_2_publish_from_edge_machine_latency"
  def chartPublishMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartPublishMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartForwardAgentNumName: String = "arktwin_center_chart_3_forward_agent_num"
  def chartForwardAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartForwardAgentNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartForwardBatchNumName: String = "arktwin_center_chart_3_forward_batch_num"
  def chartForwardBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartForwardBatchNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartForwardMachineLatencyName: String = "arktwin_center_chart_3_forward_machine_from_publish_machine_latency"
  def chartForwardMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartForwardMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartSubscribeAgentNumName: String = "arktwin_center_chart_4_subscribe_agent_num"
  def chartSubscribeAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartSubscribeAgentNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartSubscribeBatchNumName: String = "arktwin_center_chart_4_subscribe_batch_num"
  def chartSubscribeBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartSubscribeBatchNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  val chartSubscribeMachineLatencyName: String = "arktwin_center_chart_4_subscribe_from_forward_machine_latency"
  def chartSubscribeMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartSubscribeMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))
