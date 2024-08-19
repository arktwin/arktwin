// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.data.Timestamp
import arktwin.edge.actors.sinks.Clock.*
import arktwin.edge.endpoints.EdgeConfigGet
import arktwin.edge.test.ActorTestBase

class ClockSuite extends ActorTestBase:
  test(Clock.getClass.getSimpleName):
    val config = EdgeConfigGet.outExample
    val clock = testKit.spawn(Clock(config.static))
    val clockReader = testKit.createTestProbe[ClockBase]()

    clock ! Catch(ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6))
    clock ! Read(clockReader.ref)
    clockReader.receiveMessage() shouldEqual ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6)

    clock ! Catch(ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66))
    clock ! Read(clockReader.ref)
    clockReader.receiveMessage() shouldEqual ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66)
