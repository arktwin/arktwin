// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import com.typesafe.config.Config
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter
import kamon.tag.Tag

class EdgeReporter(kamon: EdgeKamon) extends MetricReporter:
  private val reportIndexes =
    import EdgeKamon.*
    Seq(endpointKey, edgeIdKey, runIdKey).zipWithIndex.toMap.withDefaultValue(Int.MaxValue)

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    snapshot.counters
      .filter: metric =>
        Seq(
          kamon.chartPublishAgentNumName,
          kamon.chartPublishBatchNumName,
          kamon.chartSubscribeAgentNumName,
          kamon.restRequestNumName,
          kamon.restAgentNumName
        ).contains(metric.name)
      .sortBy(_.name)
      .flatMap: metric =>
        metric.instruments.filter(_.value != 0).map(instrument => (metric, instrument))
      .foreach: (metric, instrument) =>
        val tags = instrument.tags
          .all()
          .sortBy(tag => reportIndexes(tag.key))
          .collect:
            case tag: Tag.String => s"${tag.key}=\"${tag.value}\""
          .mkString("{", ", ", "}")
        scribe.info(s"${metric.name}: ${instrument.value} $tags")

    snapshot.histograms
      .filter: metric =>
        Seq(
          kamon.chartPublishMachineLatencyName,
          kamon.chartSubscribeMachineLatencyName,
          kamon.restProcessMachineTimeName,
          kamon.restSimulationLatencyName
        ).contains(metric.name)
      .sortBy(_.name)
      .flatMap: metric =>
        metric.instruments.filter(_.value.count != 0).map(instrument => (metric, instrument))
      .foreach: (metric, instrument) =>
        val tags = instrument.tags
          .all()
          .sortBy(tag => reportIndexes(tag.key))
          .collect:
            case tag: Tag.String => s"${tag.key}=\"${tag.value}\""
          .mkString("{", ", ", "}")
        val summary = Seq(0, 25, 50, 75, 100).map(instrument.value.percentile(_).value).mkString(", ")
        scribe.info(s"${metric.name}: [$summary] ${metric.settings.unit.magnitude.name} $tags")

  override def stop(): Unit = {}

  override def reconfigure(newConfig: Config): Unit = {}
