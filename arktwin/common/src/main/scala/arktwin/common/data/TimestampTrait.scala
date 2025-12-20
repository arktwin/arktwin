// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.util.TimeConstants.*

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

trait TimestampTrait[Timestamp <: TimestampTrait[Timestamp]] extends Ordered[Timestamp]:
  protected def timestampCompanion: TimestampCompanionTrait[Timestamp]
  protected type Duration <: DurationTrait[Duration]
  protected def durationCompanion: DurationCompanionTrait[Duration]

  val seconds: Long
  val nanos: Int

  def +(that: Duration): Timestamp =
    timestampCompanion(
      math.addExact(seconds, that.seconds),
      nanos.toLong + that.nanos
    )

  def -(that: Timestamp): Duration =
    durationCompanion(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def -(that: Duration): Timestamp =
    timestampCompanion(
      math.subtractExact(seconds, that.seconds),
      nanos.toLong - that.nanos
    )

  def toVirtual: VirtualTimestamp =
    VirtualTimestamp(seconds, nanos)

  inline def formatIso: String =
    LocalDateTime
      .ofEpochSecond(seconds, nanos, ZoneOffset.UTC)
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  override def compare(that: Timestamp): Int =
    summon[Ordering[(Long, Int)]].compare((seconds, nanos), (that.seconds, that.nanos))

trait MachineTimestampTrait extends TimestampTrait[MachineTimestamp]:
  final override def timestampCompanion = MachineTimestamp
  final override type Duration = MachineDuration
  final override def durationCompanion = MachineDuration

trait VirtualTimestampTrait extends TimestampTrait[VirtualTimestamp]:
  final override def timestampCompanion = VirtualTimestamp
  final override type Duration = VirtualDuration
  final override def durationCompanion = VirtualDuration

trait TimestampCompanionTrait[Timestamp <: TimestampTrait[Timestamp]]:
  protected def construct(seconds: Long, nanos: Int): Timestamp

  def apply(seconds: Long, nanos: Long): Timestamp =
    var s = seconds
    var n = nanos
    if n <= -nanosPerSecond || n >= nanosPerSecond then
      s = Math.addExact(s, n / nanosPerSecond)
      n = n % nanosPerSecond
    if n < 0 then
      s = Math.subtractExact(s, 1)
      n = n + nanosPerSecond // no overflow since negative nanos is added
    construct(s, n.toInt)

  inline def apply(seconds: Long, nanos: Int): Timestamp =
    apply(seconds, nanos.toLong)

  inline def from(value: ZonedDateTime): Timestamp =
    apply(value.toEpochSecond, value.getNano)

  inline def parse(text: String): Timestamp = from(ZonedDateTime.parse(text))

  inline def parse(text: String, formatter: DateTimeFormatter): Timestamp =
    from(ZonedDateTime.parse(text, formatter))

  inline def now(): Timestamp = from(ZonedDateTime.now())

  inline def minValue: Timestamp = apply(Long.MinValue, 0)

trait MachineTimestampCompanionTrait extends TimestampCompanionTrait[MachineTimestamp]:
  final override def construct(seconds: Long, nanos: Int): MachineTimestamp =
    new MachineTimestamp(seconds, nanos)

trait VirtualTimestampCompanionTrait extends TimestampCompanionTrait[VirtualTimestamp]:
  final override def construct(seconds: Long, nanos: Int): VirtualTimestamp =
    new VirtualTimestamp(seconds, nanos)
