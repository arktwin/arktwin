// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

import TimeConstants.*

type MachineTimestamp = TaggedTimestamp[MachineTag]
object MachineTimestamp:
  inline def apply(seconds: Long, nanos: Int): MachineTimestamp =
    TaggedTimestamp.normalize(seconds, nanos)

type VirtualTimestamp = TaggedTimestamp[VirtualTag]
object VirtualTimestamp:
  inline def apply(seconds: Long, nanos: Int): VirtualTimestamp =
    TaggedTimestamp.normalize(seconds, nanos)

case class TaggedTimestamp[A <: TimeTag](seconds: Long, nanos: Int)
    extends Ordered[TaggedTimestamp[A]]:
  import TaggedTimestamp.*

  def untag: Timestamp =
    Timestamp(seconds, nanos)

  def +(that: TaggedDuration[A]): TaggedTimestamp[A] =
    normalize(
      math.addExact(seconds, that.seconds),
      nanos.toLong + that.nanos
    )

  def -(that: TaggedTimestamp[A]): TaggedDuration[A] =
    TaggedDuration.normalize(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def -(that: TaggedDuration[A]): TaggedTimestamp[A] =
    normalize(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  override def compare(that: TaggedTimestamp[A]): Int =
    val a = normalize(this)
    val b = normalize(that)
    summon[Ordering[(Long, Int)]].compare((a.seconds, a.nanos), (b.seconds, b.nanos))

  def formatIso: String =
    LocalDateTime
      .ofEpochSecond(seconds, nanos, ZoneOffset.UTC)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

object TaggedTimestamp:
  def from[A <: TimeTag](value: Timestamp): TaggedTimestamp[A] =
    TaggedTimestamp(value.seconds, value.nanos)

  def from[A <: TimeTag](value: ZonedDateTime): TaggedTimestamp[A] =
    TaggedTimestamp(value.toEpochSecond, value.getNano)

  def parse[A <: TimeTag](text: String): TaggedTimestamp[A] = from(ZonedDateTime.parse(text))

  def parse[A <: TimeTag](text: String, formatter: DateTimeFormatter): TaggedTimestamp[A] =
    from(ZonedDateTime.parse(text, formatter))

  def normalize[A <: TimeTag](seconds: Long, nanos: Long): TaggedTimestamp[A] =
    var s = seconds
    var n = nanos
    if n <= -nanosPerSecond || n >= nanosPerSecond then
      s = Math.addExact(s, n / nanosPerSecond)
      n = n % nanosPerSecond
    if n < 0 then
      s = Math.subtractExact(s, 1)
      n = n + nanosPerSecond // no overflow since negative nanos is added
    TaggedTimestamp(s, n.toInt)

  def normalize[A <: TimeTag](timestamp: TaggedTimestamp[A]): TaggedTimestamp[A] =
    normalize(timestamp.seconds, timestamp.nanos)

  def machineNow(): MachineTimestamp =
    from(ZonedDateTime.now())

  def minValue[A <: TimeTag]: TaggedTimestamp[A] = TaggedTimestamp(Long.MinValue, 0)
