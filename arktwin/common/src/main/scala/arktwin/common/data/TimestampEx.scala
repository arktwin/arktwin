// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.data.DurationEx.*

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

object TimestampEx:
  extension (a: Timestamp)
    def -(that: Timestamp): Duration = DurationEx.normalize(Duration)(
      math.subtractExact(a.seconds, that.seconds),
      a.nanos.toLong - that.nanos
    )

    def +(that: Duration): Timestamp = Timestamp.normalize(
      math.addExact(a.seconds, that.seconds),
      a.nanos.toLong + that.nanos
    )

    def -(that: Duration): Timestamp = Timestamp.normalize(
      math.subtractExact(a.seconds, that.seconds),
      a.nanos.toLong - that.nanos
    )

    def formatIso: String =
      LocalDateTime
        .ofEpochSecond(a.seconds, a.nanos, ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

  extension (a: Timestamp.type)
    def from(value: ZonedDateTime): Timestamp = Timestamp(value.toEpochSecond, value.getNano)

    def machineNow(): Timestamp = from(ZonedDateTime.now())

    def parse(text: String): Timestamp = from(ZonedDateTime.parse(text))

    def parse(text: String, formatter: DateTimeFormatter): Timestamp = from(
      ZonedDateTime.parse(text, formatter)
    )

    def normalize(seconds: Long, nanos: Long): Timestamp =
      var s = seconds
      var n = nanos
      if n <= -nanosPerSecond || n >= nanosPerSecond then
        s = Math.addExact(s, n / nanosPerSecond)
        n = n % nanosPerSecond
      if n < 0 then
        s = Math.subtractExact(s, 1)
        n = n + nanosPerSecond // no overflow since negative nanos is added
      Timestamp(s, n.toInt)

    def normalize(timestamp: Timestamp): Timestamp = normalize(timestamp.seconds, timestamp.nanos)

  val minValue: Timestamp = Timestamp(Long.MinValue, 0)

  given Ordering[Timestamp] = Ordering.by((a: Timestamp) =>
    val n = Timestamp.normalize(a)
    (n.seconds, n.nanos)
  )
