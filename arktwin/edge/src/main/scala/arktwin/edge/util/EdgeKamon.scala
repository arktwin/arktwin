/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.edge.util

import kamon.Kamon
import kamon.metric.Counter
import kamon.metric.Histogram

object EdgeKamon:
  val edgeIdTag: String = "edge_id"
  val endpointTag: String = "endpoint"

  val chartPublishAgentNumName: String = "arktwin_edge_chart_1_publish_agent_num"
  def chartPublishAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishAgentNumName)
    .withTag(edgeIdTag, edgeId)

  val chartPublishBatchNumName: String = "arktwin_edge_chart_1_publish_batch_num"
  def chartPublishBatchNumCounter(edgeId: String): Counter = Kamon
    .counter(chartPublishBatchNumName)
    .withTag(edgeIdTag, edgeId)

  val chartPublishMachineLatencyName: String = "arktwin_edge_chart_1_publish_from_put_machine_latency"
  def chartPublishMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartPublishMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTag(edgeIdTag, edgeId)

  val chartSubscribeAgentNumName: String = "arktwin_edge_chart_5_subscribe_agent_num"
  def chartSubscribeAgentNumCounter(edgeId: String): Counter = Kamon
    .counter(chartSubscribeAgentNumName)
    .withTag(edgeIdTag, edgeId)

  val chartSubscribeMachineLatencyName: String = "arktwin_edge_chart_5_subscribe_from_center_machine_latency"
  def chartSubscribeMachineLatencyHistogram(edgeId: String): Histogram = Kamon
    .histogram(chartSubscribeMachineLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTag(edgeIdTag, edgeId)

  val restRequestNumName: String = "arktwin_edge_rest_request_num"
  def restRequestNumCounter(edgeId: String, endpoint: String): Counter = Kamon
    .counter(restRequestNumName)
    .withTag(edgeIdTag, edgeId)
    .withTag(endpointTag, endpoint)

  val restAgentNumName: String = "arktwin_edge_rest_agent_num"
  def restAgentNumCounter(edgeId: String, endpoint: String): Counter = Kamon
    .counter(restAgentNumName)
    .withTag(edgeIdTag, edgeId)
    .withTag(endpointTag, endpoint)

  val restProcessMachineTimeName: String = "arktwin_edge_rest_process_machine_time"
  def restProcessMachineTimeHistogram(edgeId: String, endpoint: String): Histogram = Kamon
    .histogram(restProcessMachineTimeName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTag(edgeIdTag, edgeId)
    .withTag(endpointTag, endpoint)

  // TODO how to handle negative numbers?
  val restSimulationLatencyName: String = "arktwin_edge_rest_simulation_latency"
  def restSimulationLatencyHistogram(edgeId: String, endpoint: String): Histogram = Kamon
    .histogram(restSimulationLatencyName, kamon.metric.MeasurementUnit.time.milliseconds)
    .withTag(edgeIdTag, edgeId)
    .withTag(endpointTag, endpoint)
