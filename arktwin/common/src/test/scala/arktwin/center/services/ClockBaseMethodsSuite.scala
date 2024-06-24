/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.center.services

import arktwin.center.services.ClockBaseEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ClockBaseMethodsSuite extends AnyFunSuite with Matchers:
  test("ClockBaseMethods"):
    val timestamp = ClockBase(
      Timestamp.parse("2000-01-01T01:01:01.111+09:00"),
      Timestamp.parse("2000-01-01T01:02:01.111+09:00"),
      1.5
    ).fromMachine(Timestamp.parse("2000-01-02T01:01:01.111+09:00"))

    timestamp shouldEqual Timestamp.parse("2000-01-02T13:02:01.111+09:00")
