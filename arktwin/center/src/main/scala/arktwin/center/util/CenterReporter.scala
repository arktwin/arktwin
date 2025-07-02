// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.util

import arktwin.common.util.KamonFormatter
import com.typesafe.config.Config
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter

class CenterReporter() extends MetricReporter:
  import CenterKamon.*

  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    KamonFormatter
      .valueLogs(
        snapshot.counters
          .filter: metric =>
            Seq(
              chartPublishAgentNumName,
              chartPublishBatchNumName,
              chartRouteAgentNumName,
              chartRouteBatchNumName,
              chartSubscribeAgentNumName,
              chartSubscribeBatchNumName,
              deadLetterNumName
            ).contains(metric.name)
      )
      .foreach: metricLog =>
        if metricLog.name == CenterKamon.deadLetterNumName
        then scribe.warn(metricLog.message)
        else scribe.debug(metricLog.message)

    KamonFormatter
      .distributionLogs(
        snapshot.histograms
          .filter: metric =>
            Seq(
              chartPublishMachineLatencyName,
              chartRouteMachineLatencyName,
              chartSubscribeMachineLatencyName
            ).contains(metric.name)
      )
      .foreach: metricLog =>
        scribe.debug(metricLog.message)

  override def stop(): Unit = {}

  override def reconfigure(newConfig: Config): Unit = {}
