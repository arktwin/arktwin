// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.scalatest.funspec.AnyFunSpec

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

class TaggedTimestampSpec extends AnyFunSpec:
  describe("MachineTimestamp"):
    describe("apply"):
      it("creates normalized MachineTimestamp"):
        val timestamp = MachineTimestamp(1, 2_000_000_000)

        assert(timestamp.seconds == 3)
        assert(timestamp.nanos == 0)

  describe("VirtualTimestamp"):
    describe("apply"):
      it("creates normalized VirtualTimestamp"):
        val timestamp = VirtualTimestamp(10, -1_500_000_000)

        assert(timestamp.seconds == 8)
        assert(timestamp.nanos == 500_000_000)

  describe("TaggedTimestamp"):
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
        val t1 = TaggedTimestamp.parse[VirtualTag]("0001-01-01T00:00:00.000000000Z")
        val t2 = TaggedTimestamp.parse[VirtualTag]("9999-12-31T23:59:59.999999999Z")

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

  describe("TaggedTimestamp$"):
    describe("apply"):
      it("normalizes positive overflow nanos"):
        val result = TaggedTimestamp[MachineTag](10, 2_500_000_000L)

        assert(result.seconds == 12)
        assert(result.nanos == 500_000_000)

      it("normalizes negative nanos"):
        val result = TaggedTimestamp[VirtualTag](10, -500_000_000L)

        assert(result.seconds == 9)
        assert(result.nanos == 500_000_000)

    describe("from"):
      it("creates from Timestamp"):
        val timestamp = Timestamp(1234567890, 123456789)
        val tagged = TaggedTimestamp.from[MachineTag](timestamp)

        assert(tagged.seconds == 1234567890)
        assert(tagged.nanos == 123456789)

      it("creates from ZonedDateTime"):
        val zdt = ZonedDateTime.parse("2000-01-01T00:00:00.123456789Z")
        val tagged = TaggedTimestamp.from[VirtualTag](zdt)

        assert(tagged.seconds == zdt.toEpochSecond)
        assert(tagged.nanos == zdt.getNano)

    describe("parse"):
      it("parses ISO-8601 timestamp with timezone"):
        val result = TaggedTimestamp.parse[MachineTag]("1970-01-02T03:04:05.678+09:00")

        assert(
          result == MachineTimestamp(
            (24 + 3 - 9) * 60 * 60 + 4 * 60 + 5,
            678_000_000
          )
        )

      it("parses ISO-8601 timestamp in UTC"):
        val result = TaggedTimestamp.parse[VirtualTag]("2000-01-01T00:00:00Z")

        assert(
          result.seconds == ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond
        )
        assert(result.nanos == 0)

      it("parses timestamp with nanosecond precision"):
        val result = TaggedTimestamp.parse[MachineTag]("2000-01-01T00:00:00.123456789Z")

        assert(result.nanos == 123_456_789)

      it("handles year boundaries"):
        val t1 = TaggedTimestamp.parse[VirtualTag]("0001-01-01T00:00:00.000000000Z")
        val t2 = TaggedTimestamp.parse[VirtualTag]("9999-12-31T23:59:59.999999999Z")

        assert(t2 - t1 == VirtualDuration(315537897599L, 999999999))

      it("parses with custom DateTimeFormatter with timezone"):
        import java.util.Locale
        val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH)
        val result = TaggedTimestamp.parse[VirtualTag]("15-Jun-2023 14:30:45 JST", formatter)

        assert(
          result.seconds == ZonedDateTime
            .of(2023, 6, 15, 14, 30, 45, 0, ZoneOffset.ofHours(9))
            .toEpochSecond
        )
        assert(result.nanos == 0)

    describe("machineNow"):
      it("returns current machine time"):
        val before = Instant.now()
        val now = TaggedTimestamp.machineNow()
        val after = Instant.now()

        assert(now.seconds >= before.getEpochSecond)
        assert(now.seconds <= after.getEpochSecond)

    describe("given JsonValueCodec[VirtualTimestamp]"):
      it("serializes VirtualTimestamp to JSON"):
        val timestamp = VirtualTimestamp(1234567890L, 123456789)
        val json = writeToString(timestamp)

        assert(json == """{"seconds":1234567890,"nanos":123456789}""")

      it("deserializes JSON to VirtualTimestamp"):
        val json = """{"seconds":9876543210,"nanos":987654321}"""
        val timestamp = readFromString[VirtualTimestamp](json)

        assert(timestamp.seconds == 9876543210L)
        assert(timestamp.nanos == 987654321)

      it("throws JsonReaderException for invalid JSON"):
        val invalidJson = """{"seconds":"not a number","nanos":123}"""

        intercept[JsonReaderException]:
          readFromString[VirtualTimestamp](invalidJson)

      it("throws JsonReaderException for missing fields"):
        val incompleteJson = """{"seconds":123}"""

        intercept[JsonReaderException]:
          readFromString[VirtualTimestamp](incompleteJson)
