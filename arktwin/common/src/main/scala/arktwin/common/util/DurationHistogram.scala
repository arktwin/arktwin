// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import arktwin.common.data.*
import kamon.Kamon
import kamon.metric.MeasurementUnit
import kamon.tag.TagSet

trait DurationHistogram[Duration <: DurationTrait[Duration]]:
  protected def durationCompanion: DurationCompanionTrait[Duration]

  val name: String
  val tagSet: TagSet

  private val histogram =
    Kamon.histogram(name, MeasurementUnit.time.milliseconds).withTags(tagSet)

  def record(duration: Duration): Unit =
    if duration > durationCompanion(0, 0)
    then histogram.record(duration.millisLong)
    else histogram.record(0)

  def record(duration: Duration, num: Int): Unit =
    if duration > durationCompanion(0, 0)
    then histogram.record(duration.millisLong, num)
    else histogram.record(0, num)

class MachineDurationHistogram(override val name: String, override val tagSet: TagSet)
    extends DurationHistogram[MachineDuration]:
  final override def durationCompanion = MachineDuration

class VirtualDurationHistogram(override val name: String, override val tagSet: TagSet)
    extends DurationHistogram[VirtualDuration]:
  final override def durationCompanion = VirtualDuration
