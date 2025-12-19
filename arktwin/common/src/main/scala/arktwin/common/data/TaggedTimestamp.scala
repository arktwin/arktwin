// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.util.JsonDerivation.given
import arktwin.common.util.TimeConstants.*
import arktwin.common.util.{MachineTag, TimeTag, VirtualTag}
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

// private constructor ensures creation only through factory methods that normalize seconds/nanos
case class TaggedTimestamp[A <: TimeTag] private (seconds: Long, nanos: Int)
    extends Ordered[TaggedTimestamp[A]]:
  inline def untag: Timestamp =
    Timestamp(seconds, nanos)

  def +(that: TaggedDuration[A]): TaggedTimestamp[A] =
    TaggedTimestamp(
      math.addExact(seconds, that.seconds),
      nanos.toLong + that.nanos
    )

  def -(that: TaggedTimestamp[A]): TaggedDuration[A] =
    TaggedDuration(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def -(that: TaggedDuration[A]): TaggedTimestamp[A] =
    TaggedTimestamp(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  override def compare(that: TaggedTimestamp[A]): Int =
    summon[Ordering[(Long, Int)]].compare((seconds, nanos), (that.seconds, that.nanos))

  def formatIso: String =
    LocalDateTime
      .ofEpochSecond(seconds, nanos, ZoneOffset.UTC)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

object TaggedTimestamp:
  def apply[A <: TimeTag](seconds: Long, nanos: Long): TaggedTimestamp[A] =
    var s = seconds
    var n = nanos
    if n <= -nanosPerSecond || n >= nanosPerSecond then
      s = Math.addExact(s, n / nanosPerSecond)
      n = n % nanosPerSecond
    if n < 0 then
      s = Math.subtractExact(s, 1)
      n = n + nanosPerSecond // no overflow since negative nanos is added
    new TaggedTimestamp(s, n.toInt)

  inline def apply[A <: TimeTag](seconds: Long, nanos: Int): TaggedTimestamp[A] =
    TaggedTimestamp(seconds, nanos.toLong)

  inline def from[A <: TimeTag](value: TimestampTrait): TaggedTimestamp[A] =
    TaggedTimestamp(value.seconds, value.nanos)

  inline def from[A <: TimeTag](value: ZonedDateTime): TaggedTimestamp[A] =
    TaggedTimestamp(value.toEpochSecond, value.getNano)

  def parse[A <: TimeTag](text: String): TaggedTimestamp[A] = from(ZonedDateTime.parse(text))

  def parse[A <: TimeTag](text: String, formatter: DateTimeFormatter): TaggedTimestamp[A] =
    from(ZonedDateTime.parse(text, formatter))

  def machineNow(): MachineTimestamp =
    from(ZonedDateTime.now())

  inline def minValue[A <: TimeTag]: TaggedTimestamp[A] = TaggedTimestamp(Long.MinValue, 0)

  given JsonValueCodec[VirtualTimestamp] =
    val base = summon[JsonValueCodec[Timestamp]]
    new JsonValueCodec[VirtualTimestamp]:
      override def decodeValue(in: JsonReader, default: VirtualTimestamp): VirtualTimestamp =
        base.decodeValue(in, default.untag).tagVirtual
      override def encodeValue(x: VirtualTimestamp, out: JsonWriter): Unit =
        base.encodeValue(x.untag, out)
      override def nullValue: VirtualTimestamp = VirtualTimestamp(0, 0)

type MachineTimestamp = TaggedTimestamp[MachineTag]
object MachineTimestamp:
  inline def apply(seconds: Long, nanos: Int): MachineTimestamp =
    TaggedTimestamp(seconds, nanos)

type VirtualTimestamp = TaggedTimestamp[VirtualTag]
object VirtualTimestamp:
  inline def apply(seconds: Long, nanos: Int): VirtualTimestamp =
    TaggedTimestamp(seconds, nanos)
