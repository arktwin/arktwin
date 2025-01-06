// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TaggedDurationSuite extends AnyFunSuite with Matchers:
  test(TaggedDuration.getClass.getSimpleName):
    MachineDuration(1, 200_000_000) * 3.4 shouldEqual MachineDuration(4, 80_000_000)
    VirtualDuration(1, 200_000_000) * -3.4 shouldEqual VirtualDuration(-4, -80_000_000)
    MachineDuration(-1, -200_000_000) * 3.4 shouldEqual MachineDuration(-4, -80_000_000)
    VirtualDuration(-1, -200_000_000) * -3.4 shouldEqual VirtualDuration(4, 80_000_000)

    MachineDuration(13, 579000000) + MachineDuration(24, 680000000) shouldEqual MachineDuration(
      38,
      259000000
    )
    VirtualDuration(-38, -259000000) - VirtualDuration(
      -13,
      -579000000
    ) shouldEqual VirtualDuration(-24, -680000000)
    MachineDuration(1, 0) / MachineDuration(2, 500000000) shouldEqual 0.4 +- 1e-6
    VirtualDuration(111, 111000001) * 2.3 shouldEqual VirtualDuration(255, 555300002)
    MachineDuration(-255, -555300002) / -2.3 shouldEqual MachineDuration(111, 111000001)

    an[ArithmeticException] should be thrownBy VirtualDuration(Long.MaxValue, 0) + VirtualDuration(
      1,
      0
    )

    VirtualDuration(1, 2) < VirtualDuration(3, 4) shouldEqual true
    MachineDuration(11, 2) > MachineDuration(33, 4) shouldEqual false
