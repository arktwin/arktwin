// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.data.Timestamp
import arktwin.edge.actors.sinks.Clock.*
import arktwin.edge.endpoints.EdgeConfigGet
import arktwin.edge.test.ActorTestBase

class ClockSpec extends ActorTestBase:
  describe("Clock"):
    it("stores and retrieves clock base information"):
      val config = EdgeConfigGet.outExample
      val clock = testKit.spawn(Clock(config.static))
      val clockReader = testKit.createTestProbe[ClockBase]()

      clock ! Catch(ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6))
      clock ! Get(clockReader.ref)
      assert(clockReader.receiveMessage() == ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6))

      clock ! Catch(ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66))
      clock ! Get(clockReader.ref)
      assert(
        clockReader.receiveMessage() == ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66)
      )
