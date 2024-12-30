// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common

import arktwin.common.data.{MachineTag, TaggedDuration, TimeTag, VirtualTag}
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

class TaggedDurationHistogram[T <: TimeTag](name: String, tagSet: TagSet):
  private val histogram =
    Kamon.histogram(name, MeasurementUnit.time.milliseconds).withTags(tagSet)

  def record(duration: TaggedDuration[T]): Unit =
    if duration > TaggedDuration[T](0, 0)
    then histogram.record(duration.millisLong)
    else histogram.record(0)

  def record(duration: TaggedDuration[T], num: Int): Unit =
    if duration > TaggedDuration[T](0, 0)
    then histogram.record(duration.millisLong, num)
    else histogram.record(0, num)
