// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.util.TimeConstants.*

import scala.concurrent.duration.Duration as ScalaDuration

trait DurationTrait[Duration <: DurationTrait[Duration]] extends Ordered[Duration]:
  protected def durationCompanion: DurationCompanionTrait[Duration]

  val seconds: Long
  val nanos: Int

  def secondsDouble: Double =
    seconds + (nanos.toDouble / nanosPerSecond)

  def millisDouble: Double =
    seconds * millisPerSecond + (nanos.toDouble / nanosPerMillisecond)

  def millisLong: Long =
    seconds * millisPerSecond + (nanos / nanosPerMillisecond)

  def +(that: Duration): Duration =
    durationCompanion(
      math.addExact(seconds, that.seconds),
      nanos + that.nanos
    )

  def -(that: Duration): Duration =
    durationCompanion(
      math.subtractExact(seconds, that.seconds),
      nanos - that.nanos
    )

  def *(that: Double): Duration =
    durationCompanion.fromSecondsDouble(secondsDouble * that)

  def /(that: Duration): Double =
    secondsDouble / that.secondsDouble

  def /(that: Double): Duration =
    durationCompanion.fromSecondsDouble(secondsDouble / that)

  def toVirtual(clockSpeed: Double): VirtualDuration =
    VirtualDuration(seconds, nanos) * clockSpeed

  override def compare(that: Duration): Int =
    summon[Ordering[(Long, Int)]].compare((seconds, nanos), (that.seconds, that.nanos))

trait MachineDurationTrait extends DurationTrait[MachineDuration]:
  final override def durationCompanion = MachineDuration

trait VirtualDurationTrait extends DurationTrait[VirtualDuration]:
  final override def durationCompanion = VirtualDuration

trait DurationCompanionTrait[Duration <: DurationTrait[Duration]]:
  protected def construct(seconds: Long, nanos: Int): Duration

  def apply(seconds: Long, nanos: Long): Duration =
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
    construct(s, n.toInt)

  inline def apply(seconds: Long, nanos: Int): Duration =
    apply(seconds, nanos.toLong)

  inline def from(value: ScalaDuration): Duration =
    apply(0, value.toNanos)

  def fromSecondsDouble(seconds: Double): Duration =
    apply(seconds.toLong, math.round((seconds % 1) * nanosPerSecond).toLong)

trait MachineDurationCompanionTrait extends DurationCompanionTrait[MachineDuration]:
  final override def construct(seconds: Long, nanos: Int): MachineDuration =
    new MachineDuration(seconds, nanos)

trait VirtualDurationCompanionTrait extends DurationCompanionTrait[VirtualDuration]:
  final override def construct(seconds: Long, nanos: Int): VirtualDuration =
    new VirtualDuration(seconds, nanos)
