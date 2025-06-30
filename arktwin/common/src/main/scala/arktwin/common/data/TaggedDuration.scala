// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import scala.concurrent.duration.Duration as ScalaDuration

import TimeConstants.*

type MachineDuration = TaggedDuration[MachineTag]
object MachineDuration:
  inline def apply(seconds: Long, nanos: Int): MachineDuration =
    TaggedDuration[MachineTag](seconds, nanos)

type VirtualDuration = TaggedDuration[VirtualTag]
object VirtualDuration:
  inline def apply(seconds: Long, nanos: Int): VirtualDuration =
    TaggedDuration[VirtualTag](seconds, nanos)

// private constructor ensures creation only through factory methods that normalize seconds/nanos
case class TaggedDuration[A <: TimeTag] private (seconds: Long, nanos: Int)
    extends Ordered[TaggedDuration[A]]:
  inline def untag: Duration =
    Duration(seconds, nanos)

  def secondsDouble: Double =
    seconds + (nanos.toDouble / nanosPerSecond)

  def millisDouble: Double =
    seconds * millisPerSecond + (nanos.toDouble / nanosPerMillisecond)

  def millisLong: Long =
    seconds * millisPerSecond + (nanos / nanosPerMillisecond)

  def +(that: TaggedDuration[A]): TaggedDuration[A] =
    TaggedDuration(
      math.addExact(seconds, that.seconds),
      nanos + that.nanos
    )

  def -(that: TaggedDuration[A]): TaggedDuration[A] =
    TaggedDuration(
      math.subtractExact(seconds, that.seconds),
      nanos - that.nanos
    )

  def *(that: Double): TaggedDuration[A] =
    TaggedDuration.fromSecondsDouble(secondsDouble * that)

  def /(that: TaggedDuration[A]): Double =
    secondsDouble / that.secondsDouble

  def /(that: Double): TaggedDuration[A] =
    TaggedDuration.fromSecondsDouble(secondsDouble / that)

  override def compare(that: TaggedDuration[A]): Int =
    summon[Ordering[(Long, Int)]].compare((this.seconds, this.nanos), (that.seconds, that.nanos))

object TaggedDuration:
  def apply[A <: TimeTag](seconds: Long, nanos: Long): TaggedDuration[A] =
    var s = seconds
    var n = nanos
    if n <= -nanosPerSecond || n >= nanosPerSecond then
      s = Math.addExact(s, n / nanosPerSecond)
      n = n % nanosPerSecond
    if s > 0 && n < 0 then
      s = s - 1 // no overflow since positive seconds is decremented
      n = (n + nanosPerSecond).toInt // no overflow since negative nanos is added
    else if s < 0 && n > 0 then
      s = s + 1 // no overflow since positive seconds is incremented
      n = (n - nanosPerSecond).toInt // no overflow since positive nanos is subtracted
    new TaggedDuration(s, n.toInt)

  inline def apply[A <: TimeTag](seconds: Long, nanos: Int): TaggedDuration[A] =
    TaggedDuration(seconds, nanos.toLong)

  inline def from[A <: TimeTag](value: Duration): TaggedDuration[A] =
    TaggedDuration(value.seconds, value.nanos)

  inline def from[A <: TimeTag](value: ScalaDuration): TaggedDuration[A] =
    TaggedDuration(0, value.toNanos)

  def fromSecondsDouble[A <: TimeTag](seconds: Double): TaggedDuration[A] =
    TaggedDuration(seconds.toLong, math.round((seconds % 1) * nanosPerSecond).toLong)
