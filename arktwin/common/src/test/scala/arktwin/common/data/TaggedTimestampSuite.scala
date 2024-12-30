// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TaggedTimestampSuite extends AnyFunSuite with Matchers:
  test(TaggedTimestamp.getClass.getSimpleName):
    TaggedTimestamp.parse[MachineTag]("1970-01-02T03:04:05.678+09:00") shouldEqual MachineTimestamp(
      (24 + 3 - 9) * 60 * 60 + 4 * 60 + 5,
      678_000_000
    )

    VirtualTimestamp(123, 456789000) + VirtualDuration(
      987,
      654321000
    ) shouldEqual MachineTimestamp(1111, 111110000)
    MachineTimestamp(987, 654321000) - MachineDuration(
      1111,
      111110000
    ) shouldEqual MachineTimestamp(-124, 543211000)

    val t1: VirtualTimestamp = TaggedTimestamp.parse[VirtualTag]("0001-01-01T00:00:00.000000000Z")
    val t2: VirtualTimestamp = TaggedTimestamp.parse[VirtualTag]("9999-12-31T23:59:59.999999999Z")
    t2 - t1 shouldEqual VirtualDuration(315537897599L, 999999999)
    t1 - t2 shouldEqual VirtualDuration(-315537897599L, -999999999)

    VirtualTimestamp(1, 2) < VirtualTimestamp(3, 4) shouldEqual true
    MachineTimestamp(11, 2) > MachineTimestamp(33, 4) shouldEqual false
