/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.common.data

import arktwin.common.data.DurationEx.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DurationSuite extends AnyFunSuite with Matchers:
  test("Duration"):
    Duration(1, 200_000_000) * 3.4 shouldEqual Duration(4, 80_000_000)
    Duration(1, 200_000_000) * -3.4 shouldEqual Duration(-4, -80_000_000)
    Duration(-1, -200_000_000) * 3.4 shouldEqual Duration(-4, -80_000_000)
    Duration(-1, -200_000_000) * -3.4 shouldEqual Duration(4, 80_000_000)

    Duration(13, 579000000) + Duration(24, 680000000) shouldEqual Duration(38, 259000000)
    Duration(-38, -259000000) - Duration(-13, -579000000) shouldEqual Duration(-24, -680000000)
    Duration(1, 0) / Duration(2, 500000000) shouldEqual 0.4 +- 1e-6
    Duration(111, 111000001) * 2.3 shouldEqual Duration(255, 555300002)
    Duration(-255, -555300002) / -2.3 shouldEqual Duration(111, 111000001)

    an[ArithmeticException] should be thrownBy Duration(Long.MaxValue, 0) + Duration(1, 0)
