// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.data.Timestamp
import arktwin.common.util.LoggerConfigurator.LogLevel
import arktwin.edge.actors.sinks.Clock.*
import arktwin.edge.configs.EdgeStaticConfig
import arktwin.edge.test.ActorTestBase
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.receptionist.Receptionist

import scala.concurrent.duration.DurationInt
import scala.util.Using

class ClockSpec extends ActorTestBase:
  private val baseConfig = EdgeStaticConfig(
    actorMachineTimeout = 90.milliseconds,
    edgeIdPrefix = "edge",
    endpointMachineTimeout = 100.milliseconds,
    host = "0.0.0.0",
    port = 2237,
    portAutoIncrement = true,
    portAutoIncrementMax = 100,
    logLevel = LogLevel.Info,
    logLevelColor = true,
    logSuppressionList = Seq(),
    publishBatchSize = 100,
    publishBufferSize = 10000
  )

  describe("Clock"):
    describe("Read"):
      it("replies current clock base"):
        Using(ActorTestKit()): testKit =>
          val clockBase1 = ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6)
          val clock = testKit.spawn(Clock(clockBase1))
          val reader = testKit.createTestProbe[ClockBase]()

          clock ! Read(reader.ref)
          assert(reader.receiveMessage() == clockBase1)

          val clockBase2 = ClockBase(Timestamp(6, 5), Timestamp(4, 3), 2.1)
          clock ! UpdateClockBase(clockBase2)
          clock ! Read(reader.ref)
          assert(reader.receiveMessage() == clockBase2)

    describe("UpdateClockBase"):
      it("distributes clock base to registered observers"):
        Using(ActorTestKit()): testKit =>
          val clockBase1 = ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6)
          val clock = testKit.spawn(Clock(clockBase1))
          val observer1 = testKit.createTestProbe[UpdateClockBase]()
          val observer2 = testKit.createTestProbe[UpdateClockBase]()

          testKit.system.receptionist ! Receptionist.register(observerKey, observer1.ref)
          assert(observer1.receiveMessage().clockBase == clockBase1)

          testKit.system.receptionist ! Receptionist.register(observerKey, observer2.ref)
          assert(observer2.receiveMessage().clockBase == clockBase1)

          val clockBase2 = ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66)
          clock ! UpdateClockBase(clockBase2)
          assert(observer1.receiveMessage().clockBase == clockBase2)
          assert(observer2.receiveMessage().clockBase == clockBase2)
