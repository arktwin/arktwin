/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.util

import kamon.Kamon
import kamon.metric.Counter
import kamon.metric.Histogram

object CenterKamon:
  val edgeIdKey: String = "edge_id"

  val chartPublishAgentNumName: String = "arktwin_center_chart_2_publish_agent_num"
  def chartPublishAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishAgentNumName)
    .withTag(edgeIdKey, edgeId)

  val chartPublishBatchNumName: String = "arktwin_center_chart_2_publish_batch_num"
  def chartPublishBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishBatchNumName)
    .withTag(edgeIdKey, edgeId)

  val chartPublishMachineLatencyName: String = "arktwin_center_chart_2_publish_from_edge_machine_latency"
  def chartPublishMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartPublishMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTag(edgeIdKey, edgeId)

  val chartForwardAgentNumName: String = "arktwin_center_chart_3_forward_agent_num"
  def chartForwardAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartForwardAgentNumName)
    .withTag(edgeIdKey, edgeId)

  val chartForwardBatchNumName: String = "arktwin_center_chart_3_forward_batch_num"
  def chartForwardBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartForwardBatchNumName)
    .withTag(edgeIdKey, edgeId)

  val chartForwardMachineLatencyName: String = "arktwin_center_chart_3_forward_machine_from_publish_machine_latency"
  def chartForwardMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartForwardMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTag(edgeIdKey, edgeId)

  val chartSubscribeAgentNumName: String = "arktwin_center_chart_4_subscribe_agent_num"
  def chartSubscribeAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartSubscribeAgentNumName)
    .withTag(edgeIdKey, edgeId)

  val chartSubscribeBatchNumName: String = "arktwin_center_chart_4_subscribe_batch_num"
  def chartSubscribeBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartSubscribeBatchNumName)
    .withTag(edgeIdKey, edgeId)

  val chartSubscribeMachineLatencyName: String = "arktwin_center_chart_4_subscribe_from_forward_machine_latency"
  def chartSubscribeMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartSubscribeMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTag(edgeIdKey, edgeId)
