// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.data.{MachineTimestamp, VirtualTimestamp}
import arktwin.edge.actors.sinks.Clock.*
import arktwin.edge.test.ActorTestBase
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.receptionist.Receptionist

import scala.util.Using

class ClockSpec extends ActorTestBase:
  describe("Clock"):
    describe("Read"):
      it("replies current clock base"):
        Using(ActorTestKit()): testKit =>
          val clockBase1 = ClockBase(MachineTimestamp(1, 2), VirtualTimestamp(3, 4), 5.6)
          val clock = testKit.spawn(Clock(clockBase1))
          val reader = testKit.createTestProbe[ClockBase]()

          clock ! Read(reader.ref)
          assert(reader.receiveMessage() == clockBase1)

          val clockBase2 = ClockBase(MachineTimestamp(6, 5), VirtualTimestamp(4, 3), 2.1)
          clock ! UpdateClockBase(clockBase2)
          clock ! Read(reader.ref)
          assert(reader.receiveMessage() == clockBase2)

    describe("UpdateClockBase"):
      it("distributes clock base to registered observers"):
        Using(ActorTestKit()): testKit =>
          val clockBase1 = ClockBase(MachineTimestamp(1, 2), VirtualTimestamp(3, 4), 5.6)
          val clock = testKit.spawn(Clock(clockBase1))
          val observer1 = testKit.createTestProbe[UpdateClockBase]()
          val observer2 = testKit.createTestProbe[UpdateClockBase]()

          testKit.system.receptionist ! Receptionist.register(observerKey, observer1.ref)
          assert(observer1.receiveMessage().clockBase == clockBase1)

          testKit.system.receptionist ! Receptionist.register(observerKey, observer2.ref)
          assert(observer2.receiveMessage().clockBase == clockBase1)

          val clockBase2 = ClockBase(MachineTimestamp(11, 22), VirtualTimestamp(33, 44), 55.66)
          clock ! UpdateClockBase(clockBase2)
          assert(observer1.receiveMessage().clockBase == clockBase2)
          assert(observer2.receiveMessage().clockBase == clockBase2)
