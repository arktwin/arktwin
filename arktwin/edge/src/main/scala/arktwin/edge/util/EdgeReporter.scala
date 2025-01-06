// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import com.typesafe.config.Config
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter
import kamon.tag.Tag

class EdgeReporter() extends MetricReporter:
  private val reportIndexes =
    import EdgeKamon.*
    Seq(recipientKey, endpointKey, edgeIdKey, runIdKey).zipWithIndex.toMap
      .withDefaultValue(Int.MaxValue)

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    import EdgeKamon.*

    snapshot.counters
      .filter: metric =>
        Seq(
          chartPublishAgentNumName,
          chartPublishBatchNumName,
          chartSubscribeAgentNumName,
          deadLetterNumName,
          restRequestNumName,
          restAgentNumName
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
        val message = s"${metric.name}: ${instrument.value} $tags"
        if metric.name == deadLetterNumName then scribe.warn(message)
        else scribe.info(message)

    snapshot.histograms
      .filter: metric =>
        Seq(
          chartPublishMachineLatencyName,
          chartSubscribeMachineLatencyName,
          restProcessMachineTimeName,
          restVirtualLatencyName
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
        val summary =
          Seq(0, 25, 50, 75, 100).map(instrument.value.percentile(_).value).mkString(", ")
        scribe.info(s"${metric.name}: [$summary] ${metric.settings.unit.magnitude.name} $tags")

  override def stop(): Unit = {}

  override def reconfigure(newConfig: Config): Unit = {}
