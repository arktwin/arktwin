// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import scala.concurrent.duration.Duration as ScalaDuration

import TimeConstants.*

type MachineDuration = TaggedDuration[MachineTag]
object MachineDuration:
  inline def apply(seconds: Long, nanos: Int): MachineDuration =
    TaggedDuration.normalize[MachineTag](seconds, nanos)

type VirtualDuration = TaggedDuration[VirtualTag]
object VirtualDuration:
  inline def apply(seconds: Long, nanos: Int): VirtualDuration =
    TaggedDuration.normalize[VirtualTag](seconds, nanos)

case class TaggedDuration[A <: TimeTag](seconds: Long, nanos: Int)
    extends Ordered[TaggedDuration[A]]:
  import TaggedDuration.*

  def untag: Duration =
    Duration(seconds, nanos)

  def secondsDouble: Double =
    seconds + (nanos.toDouble / nanosPerSecond)

  def millisDouble: Double =
    seconds * millisPerSecond + (nanos.toDouble / nanosPerMillisecond)

  def millisLong: Long =
    seconds * millisPerSecond + (nanos / nanosPerMillisecond)

  def +(that: TaggedDuration[A]): TaggedDuration[A] =
    normalize(
      math.addExact(seconds, that.seconds),
      nanos.toLong + that.nanos
    )

  def -(that: TaggedDuration[A]): TaggedDuration[A] =
    normalize(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def *(that: Double): TaggedDuration[A] =
    fromSecondsDouble(secondsDouble * that)

  def /(that: TaggedDuration[A]): Double =
    secondsDouble / that.secondsDouble

  def /(that: Double): TaggedDuration[A] =
    fromSecondsDouble(secondsDouble / that)

  override def compare(that: TaggedDuration[A]): Int =
    val a = normalize(this)
    val b = normalize(that)
    summon[Ordering[(Long, Int)]].compare((a.seconds, a.nanos), (b.seconds, b.nanos))

object TaggedDuration:
  def from[A <: TimeTag](value: Duration): TaggedDuration[A] =
    normalize(value.seconds, value.nanos)

  def from[A <: TimeTag](value: ScalaDuration): TaggedDuration[A] =
    normalize(0, value.toNanos)

  def fromSecondsDouble[A <: TimeTag](seconds: Double): TaggedDuration[A] =
    normalize(seconds.toLong, math.round((seconds % 1) * nanosPerSecond).toInt)

  def normalize[A <: TimeTag](seconds: Long, nanos: Long): TaggedDuration[A] =
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
    TaggedDuration(s, n.toInt)

  def normalize[A <: TimeTag](duration: TaggedDuration[A]): TaggedDuration[A] =
    normalize(duration.seconds, duration.nanos)
