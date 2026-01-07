// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors

import arktwin.common.util.LoggerConfigurator.LogLevel
import arktwin.edge.actors.EdgeConfigurator.*
import arktwin.edge.configs.*
import arktwin.edge.configs.AxisConfig.Direction
import arktwin.edge.configs.CoordinateConfig.{LengthUnit, SpeedUnit}
import arktwin.edge.configs.EulerAnglesConfig.{AngleUnit, RotationMode, RotationOrder}
import arktwin.edge.data.Vector3
import arktwin.edge.test.ActorTestBase
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.receptionist.Receptionist

import scala.concurrent.duration.DurationInt
import scala.util.Using

class EdgeConfiguratorSpec extends ActorTestBase:
  private val baseConfig = EdgeConfig(
    dynamic = EdgeDynamicConfig(
      chart = ChartConfig(
        culling = ChartConfig.Culling(
          enabled = true,
          maxFirstAgents = 9
        ),
        expiration = ChartConfig.Expiration(
          enabled = false,
          checkMachineInterval = 1.second,
          timeout = 3.seconds
        )
      ),
      coordinate = CoordinateConfig(
        axis = AxisConfig(
          xDirection = Direction.East,
          yDirection = Direction.North,
          zDirection = Direction.Up
        ),
        centerOrigin = Vector3(0, 0, 0),
        rotation = EulerAnglesConfig(
          angleUnit = AngleUnit.Degree,
          rotationMode = RotationMode.Extrinsic,
          rotationOrder = RotationOrder.XYZ
        ),
        lengthUnit = LengthUnit.Meter,
        speedUnit = SpeedUnit.MeterPerSecond
      )
    ),
    static = EdgeStaticConfig(
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
  )

  describe("EdgeConfigurator"):
    describe("Read"):
      it("replies current edge configuration"):
        Using(ActorTestKit()): testKit =>
          val configurator = testKit.spawn(EdgeConfigurator(baseConfig))
          val reader = testKit.createTestProbe[EdgeConfig]()

          configurator ! Read(reader.ref)

          assert(reader.receiveMessage() == baseConfig)

      it("replies updated configuration after coordinate config update"):
        Using(ActorTestKit()): testKit =>
          val configurator = testKit.spawn(EdgeConfigurator(baseConfig))
          val reader = testKit.createTestProbe[EdgeConfig]()
          val newCoordinateConfig = baseConfig.dynamic.coordinate.copy(rotation = QuaternionConfig)

          configurator ! UpdateCoordinateConfig(newCoordinateConfig)
          configurator ! Read(reader.ref)

          val receivedConfig = reader.receiveMessage()
          assert(receivedConfig.dynamic.coordinate == newCoordinateConfig)
          assert(receivedConfig.dynamic.chart == baseConfig.dynamic.chart)

      it("replies updated configuration after chart config update"):
        Using(ActorTestKit()): testKit =>
          val configurator = testKit.spawn(EdgeConfigurator(baseConfig))
          val reader = testKit.createTestProbe[EdgeConfig]()
          val newChartConfig = baseConfig.dynamic.chart
            .copy(culling = baseConfig.dynamic.chart.culling.copy(maxFirstAgents = 999))

          configurator ! UpdateChartConfig(newChartConfig)
          configurator ! Read(reader.ref)

          val receivedConfig = reader.receiveMessage()
          assert(receivedConfig.dynamic.coordinate == baseConfig.dynamic.coordinate)
          assert(receivedConfig.dynamic.chart == newChartConfig)

    describe("UpdateCoordinateConfig"):
      it("distributes coordinate configuration to registered coordinate observers"):
        Using(ActorTestKit()): testKit =>
          val configurator = testKit.spawn(EdgeConfigurator(baseConfig))
          val observer1 = testKit.createTestProbe[UpdateCoordinateConfig]()
          val observer2 = testKit.createTestProbe[UpdateCoordinateConfig]()

          testKit.system.receptionist ! Receptionist.register(coordinateObserverKey, observer1.ref)
          testKit.system.receptionist ! Receptionist.register(coordinateObserverKey, observer2.ref)

          assert(observer1.receiveMessage().config == baseConfig.dynamic.coordinate)
          assert(observer2.receiveMessage().config == baseConfig.dynamic.coordinate)

          val newConfig = baseConfig.dynamic.coordinate.copy(rotation = QuaternionConfig)
          configurator ! UpdateCoordinateConfig(newConfig)

          assert(observer1.receiveMessage().config == newConfig)
          assert(observer2.receiveMessage().config == newConfig)

    describe("UpdateChartConfig"):
      it("distributes chart configuration to registered chart observers"):
        Using(ActorTestKit()): testKit =>
          val configurator = testKit.spawn(EdgeConfigurator(baseConfig))
          val observer1 = testKit.createTestProbe[UpdateChartConfig]()
          val observer2 = testKit.createTestProbe[UpdateChartConfig]()

          testKit.system.receptionist ! Receptionist.register(chartObserverKey, observer1.ref)
          testKit.system.receptionist ! Receptionist.register(chartObserverKey, observer2.ref)

          assert(observer1.receiveMessage() == UpdateChartConfig(baseConfig.dynamic.chart))
          assert(observer2.receiveMessage() == UpdateChartConfig(baseConfig.dynamic.chart))

          val newConfig = baseConfig.dynamic.chart
            .copy(culling = baseConfig.dynamic.chart.culling.copy(maxFirstAgents = 456))
          configurator ! UpdateChartConfig(newConfig)

          assert(observer1.receiveMessage() == UpdateChartConfig(newConfig))
          assert(observer2.receiveMessage() == UpdateChartConfig(newConfig))
