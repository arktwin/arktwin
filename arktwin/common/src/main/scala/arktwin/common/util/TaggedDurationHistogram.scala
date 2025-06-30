// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import arktwin.common.data.*
import kamon.Kamon
import kamon.metric.MeasurementUnit
import kamon.tag.TagSet

type MachineDurationHistogram = TaggedDurationHistogram[MachineTag]
object MachineDurationHistogram:
  inline def apply(name: String, tagSet: TagSet): MachineDurationHistogram =
    TaggedDurationHistogram[MachineTag](name, tagSet)

type VirtualDurationHistogram = TaggedDurationHistogram[VirtualTag]
object VirtualDurationHistogram:
  inline def apply(name: String, tagSet: TagSet): VirtualDurationHistogram =
    TaggedDurationHistogram[VirtualTag](name, tagSet)

class TaggedDurationHistogram[A <: TimeTag](name: String, tagSet: TagSet):
  private val histogram =
    Kamon.histogram(name, MeasurementUnit.time.milliseconds).withTags(tagSet)

  def record(duration: TaggedDuration[A]): Unit =
    if duration > TaggedDuration[A](0, 0)
    then histogram.record(duration.millisLong)
    else histogram.record(0)

  def record(duration: TaggedDuration[A], num: Int): Unit =
    if duration > TaggedDuration[A](0, 0)
    then histogram.record(duration.millisLong, num)
    else histogram.record(0, num)
