// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import org.scalatest.funspec.AnyFunSpec

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

class TimestampSpec extends AnyFunSpec:
  describe("Timestamp"):
    describe("+"):
      it("adds duration to timestamp"):
        val timestamp = VirtualTimestamp(123, 456_789_000)
        val duration = VirtualDuration(987, 654_321_000)

        assert(timestamp + duration == VirtualTimestamp(1111, 111_110_000))

      it("handles nanosecond overflow"):
        val timestamp = MachineTimestamp(100, 800_000_000)
        val duration = MachineDuration(50, 600_000_000)

        assert(timestamp + duration == MachineTimestamp(151, 400_000_000))

      it("throws ArithmeticException on overflow"):
        intercept[ArithmeticException]:
          VirtualTimestamp(Long.MaxValue, 0) + VirtualDuration(1, 0)

    describe("-"):
      it("subtracts duration from timestamp"):
        val timestamp = MachineTimestamp(987, 654_321_000)
        val duration = MachineDuration(1111, 111_110_000)

        assert(timestamp - duration == MachineTimestamp(-124, 543_211_000))

      it("handles nanosecond underflow"):
        val timestamp = VirtualTimestamp(100, 200_000_000)
        val duration = VirtualDuration(50, 700_000_000)

        assert(timestamp - duration == VirtualTimestamp(49, 500_000_000))

      it("subtracts two timestamps returning duration"):
        val t1 = VirtualTimestamp.parse("0001-01-01T00:00:00.000000000Z")
        val t2 = VirtualTimestamp.parse("9999-12-31T23:59:59.999999999Z")

        assert(t1 - t2 == VirtualDuration(-315537897599L, -999999999))

      it("operates inversely with +"):
        val timestamp = MachineTimestamp(1000, 500_000_000)
        val duration = MachineDuration(100, 250_000_000)
        val result = (timestamp + duration) - duration

        assert(result == timestamp)

      it("throws ArithmeticException on underflow"):
        intercept[ArithmeticException]:
          MachineTimestamp(Long.MinValue, 0) - MachineDuration(1, 0)

    describe("compare"):
      it("compares less than"):
        assert(VirtualTimestamp(1, 2) < VirtualTimestamp(3, 4))

      it("compares greater than"):
        assert(!(MachineTimestamp(11, 2) > MachineTimestamp(33, 4)))

      it("handles equal timestamps"):
        assert(VirtualTimestamp(42, 123_456_789) == VirtualTimestamp(42, 123_456_789))

      it("normalizes before comparison"):
        assert(MachineTimestamp(1, 1_000_000_000) == MachineTimestamp(2, 0))

      it("handles nanos-only difference"):
        assert(VirtualTimestamp(100, 123) < VirtualTimestamp(100, 456))

    describe("formatIso"):
      it("formats as ISO-8601 string"):
        assert(VirtualTimestamp(0, 0).formatIso == "1970-01-01T00:00:00")

      it("includes nanoseconds when present"):
        assert(MachineTimestamp(0, 123_456_789).formatIso == "1970-01-01T00:00:00.123456789")

      it("handles negative epoch"):
        assert(VirtualTimestamp(-86400, 0).formatIso == "1969-12-31T00:00:00")

  describe("Timestamp$"):
    describe("apply"):
      it("normalizes positive overflow nanos"):
        val timestamp = MachineTimestamp(10, 2_500_000_000L)

        assert(timestamp.seconds == 12)
        assert(timestamp.nanos == 500_000_000)

      it("normalizes negative nanos"):
        val timestamp = VirtualTimestamp(10, -500_000_000L)

        assert(timestamp.seconds == 9)
        assert(timestamp.nanos == 500_000_000)

    describe("from"):
      it("creates from ZonedDateTime"):
        val zdt = ZonedDateTime.parse("2000-01-01T00:00:00.123456789Z")
        val timestamp = VirtualTimestamp.from(zdt)

        assert(timestamp.seconds == zdt.toEpochSecond)
        assert(timestamp.nanos == zdt.getNano)

    describe("parse"):
      it("parses ISO-8601 timestamp with timezone"):
        val timestamp = MachineTimestamp.parse("1970-01-02T03:04:05.678+09:00")

        assert(
          timestamp == MachineTimestamp(
            (24 + 3 - 9) * 60 * 60 + 4 * 60 + 5,
            678_000_000
          )
        )

      it("parses ISO-8601 timestamp in UTC"):
        val timestamp = VirtualTimestamp.parse("2000-01-01T00:00:00Z")

        assert(
          timestamp.seconds == ZonedDateTime
            .of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            .toEpochSecond
        )
        assert(timestamp.nanos == 0)

      it("parses timestamp with nanosecond precision"):
        val result = MachineTimestamp.parse("2000-01-01T00:00:00.123456789Z")

        assert(result.nanos == 123_456_789)

      it("handles year boundaries"):
        val t1 = VirtualTimestamp.parse("0001-01-01T00:00:00.000000000Z")
        val t2 = VirtualTimestamp.parse("9999-12-31T23:59:59.999999999Z")

        assert(t2 - t1 == VirtualDuration(315537897599L, 999999999))

      it("parses with custom DateTimeFormatter with timezone"):
        import java.util.Locale
        val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH)
        val timestamp = VirtualTimestamp.parse("15-Jun-2023 14:30:45 JST", formatter)

        assert(
          timestamp.seconds == ZonedDateTime
            .of(2023, 6, 15, 14, 30, 45, 0, ZoneOffset.ofHours(9))
            .toEpochSecond
        )
        assert(timestamp.nanos == 0)

    describe("machineNow"):
      it("returns current machine time"):
        val before = Instant.now()
        val now = MachineTimestamp.now()
        val after = Instant.now()

        assert(now.seconds >= before.getEpochSecond)
        assert(now.seconds <= after.getEpochSecond)
