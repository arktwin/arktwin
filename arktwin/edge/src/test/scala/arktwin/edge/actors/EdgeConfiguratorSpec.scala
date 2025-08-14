// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
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
    dynamic = DynamicEdgeConfig(
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
      ),
      culling = CullingConfig(
        edgeCulling = true,
        maxFirstAgents = 9
      )
    ),
    static = StaticEdgeConfig(
      edgeIdPrefix = "edge",
      host = "0.0.0.0",
      port = 2237,
      portAutoIncrement = true,
      portAutoIncrementMax = 100,
      logLevel = LogLevel.Info,
      logLevelColor = true,
      logSuppressionList = Seq(),
      actorTimeout = 90.milliseconds,
      endpointTimeout = 100.milliseconds,
      clockInitialStashSize = 100,
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
          assert(receivedConfig.dynamic.culling == baseConfig.dynamic.culling)

      it("replies updated configuration after culling config update"):
        Using(ActorTestKit()): testKit =>
          val configurator = testKit.spawn(EdgeConfigurator(baseConfig))
          val reader = testKit.createTestProbe[EdgeConfig]()
          val newCullingConfig = baseConfig.dynamic.culling.copy(maxFirstAgents = 999)

          configurator ! UpdateCullingConfig(newCullingConfig)
          configurator ! Read(reader.ref)

          val receivedConfig = reader.receiveMessage()
          assert(receivedConfig.dynamic.coordinate == baseConfig.dynamic.coordinate)
          assert(receivedConfig.dynamic.culling == newCullingConfig)

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

    describe("UpdateCullingConfig"):
      it("distributes culling configuration to registered culling observers"):
        Using(ActorTestKit()): testKit =>
          val configurator = testKit.spawn(EdgeConfigurator(baseConfig))
          val observer1 = testKit.createTestProbe[UpdateCullingConfig]()
          val observer2 = testKit.createTestProbe[UpdateCullingConfig]()

          testKit.system.receptionist ! Receptionist.register(cullingObserverKey, observer1.ref)
          testKit.system.receptionist ! Receptionist.register(cullingObserverKey, observer2.ref)

          assert(observer1.receiveMessage() == UpdateCullingConfig(baseConfig.dynamic.culling))
          assert(observer2.receiveMessage() == UpdateCullingConfig(baseConfig.dynamic.culling))

          val newConfig = baseConfig.dynamic.culling.copy(maxFirstAgents = 456)
          configurator ! UpdateCullingConfig(newConfig)

          assert(observer1.receiveMessage() == UpdateCullingConfig(newConfig))
          assert(observer2.receiveMessage() == UpdateCullingConfig(newConfig))
