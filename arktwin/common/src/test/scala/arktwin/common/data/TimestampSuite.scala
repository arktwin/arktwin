// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.data.TimestampEx.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TimestampSuite extends AnyFunSuite with Matchers:
  test(Timestamp.getClass.getSimpleName):
    Timestamp.parse("1970-01-02T03:04:05.678+09:00") shouldEqual Timestamp(
      (24 + 3 - 9) * 60 * 60 + 4 * 60 + 5,
      678_000_000
    )

    Timestamp(123, 456789000) + Duration(987, 654321000) shouldEqual Timestamp(1111, 111110000)
    Timestamp(987, 654321000) - Duration(1111, 111110000) shouldEqual Timestamp(-124, 543211000)

    val t1: Timestamp = Timestamp.parse("0001-01-01T00:00:00.000000000Z")
    val t2: Timestamp = Timestamp.parse("9999-12-31T23:59:59.999999999Z")
    t2 - t1 shouldEqual Duration(315537897599L, 999999999)
    t1 - t2 shouldEqual Duration(-315537897599L, -999999999)
