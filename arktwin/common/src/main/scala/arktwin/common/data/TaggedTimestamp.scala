// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

type MachineTimestamp = TaggedTimestamp[MachineTag]
object MachineTimestamp:
  inline def apply(seconds: Long, nanos: Int): MachineTimestamp =
    TaggedTimestamp.normalize(seconds, nanos)

type VirtualTimestamp = TaggedTimestamp[VirtualTag]
object VirtualTimestamp:
  inline def apply(seconds: Long, nanos: Int): VirtualTimestamp =
    TaggedTimestamp.normalize(seconds, nanos)

case class TaggedTimestamp[T <: TimeTag](seconds: Long, nanos: Int)
    extends Ordered[TaggedTimestamp[T]]:
  import TaggedTimestamp.*

  def compare(that: TaggedTimestamp[T]): Int =
    val a = normalize(this)
    val b = normalize(that)
    summon[Ordering[(Long, Int)]].compare((a.seconds, a.nanos), (b.seconds, b.nanos))

  def untag: Timestamp =
    Timestamp(seconds, nanos)

  def -(that: TaggedTimestamp[T]): TaggedDuration[T] =
    TaggedDuration.normalize(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def +(that: TaggedDuration[T]): TaggedTimestamp[T] =
    normalize(
      math.addExact(seconds, that.seconds),
      nanos.toLong + that.nanos
    )

  def -(that: TaggedDuration[T]): TaggedTimestamp[T] =
    normalize(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def formatIso: String =
    LocalDateTime
      .ofEpochSecond(seconds, nanos, ZoneOffset.UTC)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

object TaggedTimestamp:
  def from[T <: TimeTag](value: Timestamp): TaggedTimestamp[T] =
    TaggedTimestamp(value.seconds, value.nanos)

  def from[T <: TimeTag](value: ZonedDateTime): TaggedTimestamp[T] =
    TaggedTimestamp(value.toEpochSecond, value.getNano)

  def machineNow(): MachineTimestamp =
    from(ZonedDateTime.now())

  def parse[T <: TimeTag](text: String): TaggedTimestamp[T] = from(ZonedDateTime.parse(text))

  def parse[T <: TimeTag](text: String, formatter: DateTimeFormatter): TaggedTimestamp[T] =
    from(ZonedDateTime.parse(text, formatter))

  def normalize[T <: TimeTag](seconds: Long, nanos: Long): TaggedTimestamp[T] =
    var s = seconds
    var n = nanos
    if n <= -TaggedDuration.nanosPerSecond || n >= TaggedDuration.nanosPerSecond then
      s = Math.addExact(s, n / TaggedDuration.nanosPerSecond)
      n = n % TaggedDuration.nanosPerSecond
    if n < 0 then
      s = Math.subtractExact(s, 1)
      n = n + TaggedDuration.nanosPerSecond // no overflow since negative nanos is added
    TaggedTimestamp(s, n.toInt)

  def normalize[T <: TimeTag](timestamp: TaggedTimestamp[T]): TaggedTimestamp[T] =
    normalize(timestamp.seconds, timestamp.nanos)

  def minValue[T <: TimeTag]: TaggedTimestamp[T] = TaggedTimestamp(Long.MinValue, 0)
