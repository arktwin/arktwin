// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import arktwin.common.util.{MachineDurationHistogram, VirtualDurationHistogram}
import kamon.Kamon
import kamon.metric.Counter
import kamon.tag.TagSet

class EdgeKamon(runId: String, edgeId: String):
  import EdgeKamon.*
  import arktwin.common.util.KamonTagKeys.*
  val commonTags = TagSet.of(runIdKey, runId).withTag(edgeIdKey, edgeId)

  def chartPublishAgentNumCounter(): Counter = Kamon
    .counter(chartPublishAgentNumName)
    .withTags(commonTags)

  def chartPublishBatchNumCounter(): Counter = Kamon
    .counter(chartPublishBatchNumName)
    .withTags(commonTags)

  def chartPublishMachineLatencyHistogram(): MachineDurationHistogram =
    MachineDurationHistogram(chartPublishMachineLatencyName, commonTags)

  def chartSubscribeAgentNumCounter(): Counter = Kamon
    .counter(chartSubscribeAgentNumName)
    .withTags(commonTags)

  def chartSubscribeMachineLatencyHistogram(): MachineDurationHistogram =
    MachineDurationHistogram(chartSubscribeMachineLatencyName, commonTags)

  def deadLetterNumCounter(): Counter = Kamon
    .counter(deadLetterNumName)
    .withTags(commonTags)

  def restRequestNumCounter(endpoint: String): Counter = Kamon
    .counter(restRequestNumName)
    .withTags(commonTags.withTag(endpointKey, endpoint))

  def restAgentNumCounter(endpoint: String): Counter = Kamon
    .counter(restAgentNumName)
    .withTags(commonTags.withTag(endpointKey, endpoint))

  def restProcessMachineTimeHistogram(endpoint: String): MachineDurationHistogram =
    MachineDurationHistogram(restProcessMachineTimeName, commonTags.withTag(endpointKey, endpoint))

  // TODO how to handle negative numbers?
  def restVirtualLatencyHistogram(endpoint: String): VirtualDurationHistogram =
    VirtualDurationHistogram(restVirtualLatencyName, commonTags.withTag(endpointKey, endpoint))

object EdgeKamon:
  inline val chartPublishAgentNumName = "arktwin_edge_chart_1_publish_agent_num"
  inline val chartPublishBatchNumName = "arktwin_edge_chart_1_publish_batch_num"
  inline val chartPublishMachineLatencyName =
    "arktwin_edge_chart_1_publish_from_put_machine_latency"
  inline val chartSubscribeAgentNumName = "arktwin_edge_chart_5_subscribe_agent_num"
  inline val chartSubscribeMachineLatencyName =
    "arktwin_edge_chart_5_subscribe_from_center_machine_latency"
  inline val deadLetterNumName = "arktwin_edge_dead_letter_num"
  inline val restRequestNumName = "arktwin_edge_rest_request_num"
  inline val restAgentNumName = "arktwin_edge_rest_agent_num"
  inline val restProcessMachineTimeName = "arktwin_edge_rest_process_machine_time"
  inline val restVirtualLatencyName = "arktwin_edge_rest_latency"
