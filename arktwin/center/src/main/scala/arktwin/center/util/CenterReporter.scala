/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.util

import com.typesafe.config.Config
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter
import kamon.tag.Tag
import org.slf4j.Logger

case class CenterReporter(log: Logger) extends MetricReporter:
  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    snapshot.counters
      .filter: metric =>
        Seq(
          CenterKamon.chartPublishAgentNumName,
          CenterKamon.chartPublishBatchNumName,
          CenterKamon.chartForwardAgentNumName,
          CenterKamon.chartForwardBatchNumName,
          CenterKamon.chartSubscribeAgentNumName,
          CenterKamon.chartSubscribeBatchNumName
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
          CenterKamon.chartPublishMachineLatencyName,
          CenterKamon.chartForwardMachineLatencyName,
          CenterKamon.chartSubscribeMachineLatencyName
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
