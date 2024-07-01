/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.edge.util

import com.typesafe.config.Config
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter
import kamon.tag.Tag
import org.slf4j.Logger

case class EdgeReporter(kamon: EdgeKamon, log: Logger) extends MetricReporter:
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
          .sortBy(_.key)
          .collect:
            case tag: Tag.String => s"${tag.key}=\"${tag.value}\""
          .mkString("{", ",", "}")
        log.info(s"${metric.name} $tags: ${instrument.value}")

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
          .sortBy(_.key)
          .collect:
            case tag: Tag.String => s"${tag.key}=\"${tag.value}\""
          .mkString("{", ",", "}")
        val summary = Seq(0, 25, 50, 75, 100).map(instrument.value.percentile(_).value).mkString(", ")
        log.info(s"${metric.name} $tags: [$summary] ${metric.settings.unit.magnitude.name}")

  override def stop(): Unit = {}

  override def reconfigure(newConfig: Config): Unit = {}
