// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.util

import arktwin.common.MachineDurationHistogram
import kamon.Kamon
import kamon.metric.Counter
import kamon.tag.TagSet

object CenterKamon:
  val runIdKey: String = "run_id"
  val edgeIdKey: String = "edge_id"
  val recipientKey: String = "recipient"

  val chartPublishAgentNumName: String = "arktwin_center_chart_2_publish_agent_num"
  val chartPublishBatchNumName: String = "arktwin_center_chart_2_publish_batch_num"
  val chartPublishMachineLatencyName: String =
    "arktwin_center_chart_2_publish_from_edge_machine_latency"
  val chartRouteAgentNumName: String = "arktwin_center_chart_3_route_agent_num"
  val chartRouteBatchNumName: String = "arktwin_center_chart_3_route_batch_num"
  val chartRouteMachineLatencyName: String =
    "arktwin_center_chart_3_route_machine_from_publish_machine_latency"
  val chartSubscribeAgentNumName: String = "arktwin_center_chart_4_subscribe_agent_num"
  val chartSubscribeBatchNumName: String = "arktwin_center_chart_4_subscribe_batch_num"
  val chartSubscribeMachineLatencyName: String =
    "arktwin_center_chart_4_subscribe_from_route_machine_latency"
  val deadLetterNumName: String = "arktwin_center_dead_letter_num"

class CenterKamon(runId: String):
  import CenterKamon.*
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
