// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.util

import arktwin.common.util.MachineDurationHistogram
import kamon.Kamon
import kamon.metric.Counter
import kamon.tag.TagSet

class CenterKamon(runId: String):
  import CenterKamon.*
  import arktwin.common.util.KamonTagKeys.*

  val commonTags = TagSet.of(runIdKey, runId)

  def chartPublishAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishAgentNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  def chartPublishBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishBatchNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  def chartPublishMachineLatencyHistogram(edgeId: String): MachineDurationHistogram =
    MachineDurationHistogram(chartPublishMachineLatencyName, commonTags.withTag(edgeIdKey, edgeId))

  def chartRouteAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartRouteAgentNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  def chartRouteBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartRouteBatchNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  def chartRouteMachineLatencyHistogram(edgeId: String): MachineDurationHistogram =
    MachineDurationHistogram(
      chartRouteMachineLatencyName,
      commonTags.withTag(edgeIdKey, edgeId)
    )

  def chartSubscribeAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartSubscribeAgentNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  def chartSubscribeBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartSubscribeBatchNumName)
    .withTags(commonTags.withTag(edgeIdKey, edgeId))

  def chartSubscribeMachineLatencyHistogram(edgeId: String): MachineDurationHistogram =
    MachineDurationHistogram(
      chartSubscribeMachineLatencyName,
      commonTags.withTag(edgeIdKey, edgeId)
    )

  def deadLetterNumCounter(): Counter = Kamon
    .counter(deadLetterNumName)
    .withTags(commonTags)

object CenterKamon:
  inline val chartPublishAgentNumName = "arktwin_center_chart_2_publish_agent_num"
  inline val chartPublishBatchNumName = "arktwin_center_chart_2_publish_batch_num"
  inline val chartPublishMachineLatencyName =
    "arktwin_center_chart_2_publish_from_edge_machine_latency"
  inline val chartRouteAgentNumName = "arktwin_center_chart_3_route_agent_num"
  inline val chartRouteBatchNumName = "arktwin_center_chart_3_route_batch_num"
  inline val chartRouteMachineLatencyName =
    "arktwin_center_chart_3_route_machine_from_publish_machine_latency"
  inline val chartSubscribeAgentNumName = "arktwin_center_chart_4_subscribe_agent_num"
  inline val chartSubscribeBatchNumName = "arktwin_center_chart_4_subscribe_batch_num"
  inline val chartSubscribeMachineLatencyName =
    "arktwin_center_chart_4_subscribe_from_route_machine_latency"
  inline val deadLetterNumName = "arktwin_center_dead_letter_num"
