// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ClockBase
import arktwin.common.data.Timestamp
import arktwin.common.util.LoggerConfigurator.LogLevel
import arktwin.edge.actors.sinks.Clock.*
import arktwin.edge.configs.StaticEdgeConfig
import arktwin.edge.test.ActorTestBase
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.receptionist.Receptionist

import scala.concurrent.duration.DurationInt
import scala.util.Using

class ClockSpec extends ActorTestBase:
  private val baseConfig = StaticEdgeConfig(
    actorMachineTimeout = 90.milliseconds,
    clockInitialStashSize = 100,
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
          val clock = testKit.spawn(Clock(baseConfig))
          val reader = testKit.createTestProbe[ClockBase]()

          val clockBase = ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6)
          clock ! UpdateClockBase(clockBase)
          clock ! Read(reader.ref)

          assert(reader.receiveMessage() == clockBase)

      it("replies a clock base after first update"):
        Using(ActorTestKit()): testKit =>
          val clock = testKit.spawn(Clock(baseConfig))
          val reader = testKit.createTestProbe[ClockBase]()

          clock ! Read(reader.ref)
          val clockBase = ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6)
          clock ! UpdateClockBase(clockBase)

          assert(reader.receiveMessage() == clockBase)

    describe("UpdateClockBase"):
      it("distributes clock base to registered observers"):
        Using(ActorTestKit()): testKit =>
          val clock = testKit.spawn(Clock(baseConfig))
          val observer1 = testKit.createTestProbe[UpdateClockBase]()
          val observer2 = testKit.createTestProbe[UpdateClockBase]()

          testKit.system.receptionist ! Receptionist.register(observerKey, observer1.ref)

          val clockBase1 = ClockBase(Timestamp(1, 2), Timestamp(3, 4), 5.6)
          clock ! UpdateClockBase(clockBase1)
          assert(observer1.receiveMessage().clockBase == clockBase1)

          testKit.system.receptionist ! Receptionist.register(observerKey, observer2.ref)
          assert(observer2.receiveMessage().clockBase == clockBase1)

          val clockBase2 = ClockBase(Timestamp(11, 22), Timestamp(33, 44), 55.66)
          clock ! UpdateClockBase(clockBase2)
          assert(observer1.receiveMessage().clockBase == clockBase2)
          assert(observer2.receiveMessage().clockBase == clockBase2)
