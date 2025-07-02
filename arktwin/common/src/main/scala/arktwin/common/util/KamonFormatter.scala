// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import arktwin.common.util.KamonTagKeys.*
import kamon.metric.{MeasurementUnit, MetricSnapshot}
import kamon.tag.Tag

object KamonFormatter:
  private val reportIndexes =
    Seq(recipientKey, endpointKey, edgeIdKey, runIdKey).zipWithIndex.toMap
      .withDefaultValue(Int.MaxValue)

  case class MetricLog(name: String, message: String)

  def valueLogs[A](metricSnapshots: Seq[MetricSnapshot.Values[A]]): Seq[MetricLog] =
    metricSnapshots
      .sortBy(_.name)
      .flatMap: metric =>
        metric.instruments.filter(_.value != 0).map(instrument => (metric, instrument))
      .map: (metric, instrument) =>
        val tags = instrument.tags
          .all()
          .sortBy(tag => reportIndexes(tag.key))
          .map(tag => s"""${tag.key}="${Tag.unwrapValue(tag)}"""")
          .mkString("{", ", ", "}")
        MetricLog(metric.name, s"${metric.name}: ${instrument.value} $tags")

  def distributionLogs(metricSnapshots: Seq[MetricSnapshot.Distributions]): Seq[MetricLog] =
    metricSnapshots
      .sortBy(_.name)
      .flatMap: metric =>
        metric.instruments.filter(_.value.count != 0).map(instrument => (metric, instrument))
      .map: (metric, instrument) =>
        val tags = instrument.tags
          .all()
          .sortBy(tag => reportIndexes(tag.key))
          .map(tag => s"""${tag.key}="${Tag.unwrapValue(tag)}"""")
          .mkString("{", ", ", "}")
        val summary =
          Seq(0, 25, 50, 75, 100).map(instrument.value.percentile(_).value).mkString(", ")
        val unitName =
          metric.settings.unit match
            case MeasurementUnit.none => ""
            case _                    => s" ${metric.settings.unit.magnitude.name}"
        MetricLog(
          metric.name,
          s"${metric.name}: [$summary]$unitName $tags"
        )
