// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration as ScalaDuration

class DurationSpec extends AnyFunSpec with Matchers:
  given Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-6)

  describe("Duration"):
    describe("secondsDouble"):
      it("converts to double seconds"):
        assert(VirtualDuration(10, 500_000_000).secondsDouble === 10.5)

      it("handles negative duration for secondsDouble"):
        assert(MachineDuration(-5, -250_000_000).secondsDouble === -5.25)

    describe("millisDouble"):
      it("converts to double milliseconds"):
        assert(VirtualDuration(1, 500_000_000).millisDouble === 1500.0)

    describe("millisLong"):
      it("converts to long milliseconds"):
        assert(MachineDuration(2, 750_000_000).millisLong === 2750L)

    describe("+"):
      it("adds two positive durations"):
        val d1 = MachineDuration(13, 579_000_000)
        val d2 = MachineDuration(24, 680_000_000)

        assert(d1 + d2 == MachineDuration(38, 259_000_000))

      it("handles nanosecond overflow"):
        val d1 = VirtualDuration(1, 800_000_000)
        val d2 = VirtualDuration(2, 600_000_000)

        assert(d1 + d2 == VirtualDuration(4, 400_000_000))

      it("throws ArithmeticException on overflow"):
        val d1 = VirtualDuration(Long.MaxValue, 0)
        val d2 = VirtualDuration(1, 0)

        intercept[ArithmeticException]:
          d1 + d2

    describe("-"):
      it("subtracts two durations"):
        val d1 = VirtualDuration(-38, -259_000_000)
        val d2 = VirtualDuration(-13, -579_000_000)

        assert(d1 - d2 == VirtualDuration(-24, -680_000_000))

      it("handles nanosecond underflow"):
        val d1 = MachineDuration(5, 200_000_000)
        val d2 = MachineDuration(2, 700_000_000)

        assert(d1 - d2 == MachineDuration(2, 500_000_000))

      it("throws ArithmeticException on underflow"):
        val d1 = MachineDuration(Long.MinValue, 0)
        val d2 = MachineDuration(1, 0)

        intercept[ArithmeticException]:
          d1 - d2

    describe("*"):
      it("multiplies duration by positive scalar"):
        assert(MachineDuration(1, 200_000_000) * 3.4 == MachineDuration(4, 80_000_000))

      it("multiplies duration by negative scalar"):
        assert(VirtualDuration(1, 200_000_000) * -3.4 == VirtualDuration(-4, -80_000_000))

      it("handles negative duration with positive scalar"):
        assert(MachineDuration(-1, -200_000_000) * 3.4 == MachineDuration(-4, -80_000_000))

      it("handles negative duration with negative scalar"):
        assert(VirtualDuration(-1, -200_000_000) * -3.4 == VirtualDuration(4, 80_000_000))

      it("multiplies with high precision"):
        assert(VirtualDuration(111, 111_000_001) * 2.3 == VirtualDuration(255, 555_300_002))

      it("multiplies by zero"):
        assert(MachineDuration(100, 500_000_000) * 0.0 == MachineDuration(0, 0))

    describe("/"):
      it("divides duration by scalar"):
        assert(MachineDuration(-255, -555_300_002) / -2.3 == MachineDuration(111, 111_000_001))

      it("divides by one returns same duration"):
        val duration = VirtualDuration(42, 123_456_789)

        assert(duration / 1.0 == duration)

      it("divides duration by duration"):
        val d1 = MachineDuration(1, 0)
        val d2 = MachineDuration(2, 500_000_000)

        assert(d1 / d2 === 0.4)

      it("handles division by zero duration"):
        val d1 = VirtualDuration(1, 0)
        val d2 = VirtualDuration(0, 0)

        assert((d1 / d2).isInfinity)

    describe("compare"):
      it("compares less than"):
        assert(VirtualDuration(1, 2) < VirtualDuration(3, 4))

      it("compares greater than"):
        assert(!(MachineDuration(11, 2) > MachineDuration(33, 4)))

      it("handles equal durations"):
        assert(VirtualDuration(5, 500_000_000) == VirtualDuration(5, 500_000_000))

      it("normalizes before comparison"):
        assert(MachineDuration(1, 1_000_000_000) == MachineDuration(2, 0))

  describe("Duration$"):
    describe("apply"):
      it("normalizes positive overflow nanos"):
        val duration = MachineDuration(1, 1_500_000_000L)

        assert(duration.seconds == 2)
        assert(duration.nanos == 500_000_000)

      it("normalizes negative overflow nanos"):
        val duration = VirtualDuration(1, -1_500_000_000L)

        assert(duration.seconds == 0)
        assert(duration.nanos == -500_000_000)

      it("handles positive seconds with negative nanos"):
        val duration = MachineDuration(5, -200_000_000)

        assert(duration.seconds == 4)
        assert(duration.nanos == 800_000_000)

      it("handles negative seconds with positive nanos"):
        val duration = VirtualDuration(-5, 200_000_000)

        assert(duration.seconds == -4)
        assert(duration.nanos == -800_000_000)

    describe("from"):
      it("creates from Scala Duration"):
        val scalaDuration = ScalaDuration("2.5 seconds")
        val duration = VirtualDuration.from(scalaDuration)

        assert(duration.seconds == 2)
        assert(duration.nanos == 500_000_000)

    describe("fromSecondsDouble"):
      it("converts from double seconds"):
        val duration = MachineDuration.fromSecondsDouble(3.75)

        assert(duration.seconds == 3)
        assert(duration.nanos == 750_000_000)

      it("handles negative values for fromSecondsDouble"):
        val duration = VirtualDuration.fromSecondsDouble(-1.25)

        assert(duration.seconds == -1)
        assert(duration.nanos == -250_000_000)
