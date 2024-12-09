// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.services.ClockBaseEx.*
import arktwin.common.data.{MachineTag, TaggedTimestamp, VirtualTag}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ClockBaseSuite extends AnyFunSuite with Matchers:
  test(ClockBase.getClass.getSimpleName):
    val timestamp = ClockBase(
      TaggedTimestamp.parse[MachineTag]("2000-01-01T01:01:01.111+09:00"),
      TaggedTimestamp.parse[VirtualTag]("2000-01-01T01:02:01.111+09:00"),
      1.5
    ).fromMachine(TaggedTimestamp.parse[MachineTag]("2000-01-02T01:01:01.111+09:00"))

    timestamp shouldEqual TaggedTimestamp.parse[VirtualTag]("2000-01-02T13:02:01.111+09:00")
