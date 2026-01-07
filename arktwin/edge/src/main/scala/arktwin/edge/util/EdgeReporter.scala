// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import arktwin.common.util.KamonFormatter
import com.typesafe.config.Config
import kamon.metric.PeriodSnapshot
import kamon.module.MetricReporter

class EdgeReporter() extends MetricReporter:
  override def reportPeriodSnapshot(snapshot: PeriodSnapshot): Unit =
    import EdgeKamon.*

    KamonFormatter
      .valueLogs(
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
      )
      .foreach: metricLog =>
        if metricLog.name == deadLetterNumName then scribe.warn(metricLog.message)
        else scribe.debug(metricLog.message)

    KamonFormatter
      .distributionLogs(
        snapshot.histograms
          .filter: metric =>
            Seq(
              chartPublishMachineLatencyName,
              chartSubscribeMachineLatencyName,
              restProcessMachineTimeName,
              restVirtualLatencyName
            ).contains(metric.name)
      )
      .foreach: metricLog =>
        scribe.debug(metricLog.message)

  override def stop(): Unit = {}

  override def reconfigure(newConfig: Config): Unit = {}
