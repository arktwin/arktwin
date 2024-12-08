// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import scala.concurrent.duration.Duration as ScalaDuration

type MachineDuration = TaggedDuration[MachineTag]
object MachineDuration:
  inline def apply(seconds: Long, nanos: Int): MachineDuration =
    TaggedDuration[MachineTag](seconds, nanos)

type VirtualDuration = TaggedDuration[VirtualTag]
object VirtualDuration:
  inline def apply(seconds: Long, nanos: Int): VirtualDuration =
    TaggedDuration[VirtualTag](seconds, nanos)

case class TaggedDuration[T <: TimeTag](seconds: Long, nanos: Int):
  import TaggedDuration.*

  def untag: Duration =
    Duration(seconds, nanos)

  def secondsDouble: Double =
    seconds + (nanos.toDouble / nanosPerSecond)

  def millisDouble: Double =
    seconds * millisPerSecond + (nanos.toDouble / nanosPerMillisecond)

  def millisLong: Long =
    seconds * millisPerSecond + (nanos / nanosPerMillisecond)

  def +(that: TaggedDuration[T]): TaggedDuration[T] =
    normalize(
      math.addExact(seconds, that.seconds),
      nanos.toLong + that.nanos
    )

  def -(that: TaggedDuration[T]): TaggedDuration[T] =
    normalize(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def /(that: TaggedDuration[T]): Double =
    secondsDouble / that.secondsDouble

  def *(that: Double): TaggedDuration[T] =
    fromSecondsDouble(secondsDouble * that)

  def /(that: Double): TaggedDuration[T] =
    fromSecondsDouble(secondsDouble / that)

object TaggedDuration:
  def from[T <: TimeTag](value: Duration): TaggedDuration[T] =
    normalize(value.seconds, value.nanos)

  def from[T <: TimeTag](value: ScalaDuration): TaggedDuration[T] =
    normalize(0, value.toNanos)

  def normalize[T <: TimeTag](seconds: Long, nanos: Long): TaggedDuration[T] =
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

  def normalize[T <: TimeTag](duration: TaggedDuration[T]): TaggedDuration[T] =
    normalize(duration.seconds, duration.nanos)

  def fromSecondsDouble[T <: TimeTag](seconds: Double): TaggedDuration[T] =
    normalize(seconds.toLong, math.round((seconds % 1) * nanosPerSecond).toInt)

  inline val nanosPerSecond = 1000000000L
  inline val nanosPerMillisecond = 1000000L
  inline val nanosPerMicrosecond = 1000L
  inline val millisPerSecond = 1000L
  inline val microsPerSeconds = 1000000L

  given [T <: TimeTag]: Ordering[TaggedDuration[T]] = Ordering.by((a: TaggedDuration[T]) =>
    val d = TaggedDuration.normalize(a)
    (d.seconds, d.nanos)
  )
