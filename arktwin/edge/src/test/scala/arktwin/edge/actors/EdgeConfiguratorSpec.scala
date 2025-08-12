// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors

import arktwin.edge.actors.EdgeConfigurator.*
import arktwin.edge.configs.{EdgeConfig, QuaternionConfig}
import arktwin.edge.endpoints.EdgeConfigGet
import arktwin.edge.test.ActorTestBase
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.receptionist.Receptionist

import scala.util.Using

class EdgeConfiguratorSpec extends ActorTestBase:
  describe("EdgeConfigurator"):
    describe("Read"):
      it("replies current edge configuration"):
        Using(ActorTestKit()): testKit =>
          val config = EdgeConfigGet.outExample
          val configurator = testKit.spawn(EdgeConfigurator(config))
          val reader = testKit.createTestProbe[EdgeConfig]()

          configurator ! Read(reader.ref)

          assert(reader.receiveMessage() == config)

      it("replies updated configuration after coordinate config update"):
        Using(ActorTestKit()): testKit =>
          val config = EdgeConfigGet.outExample
          val configurator = testKit.spawn(EdgeConfigurator(config))
          val reader = testKit.createTestProbe[EdgeConfig]()
          val newCoordinateConfig = config.dynamic.coordinate.copy(rotation = QuaternionConfig)

          configurator ! UpdateCoordinateConfig(newCoordinateConfig)
          configurator ! Read(reader.ref)

          val receivedConfig = reader.receiveMessage()
          assert(receivedConfig.dynamic.coordinate == newCoordinateConfig)
          assert(receivedConfig.dynamic.culling == config.dynamic.culling)

      it("replies updated configuration after culling config update"):
        Using(ActorTestKit()): testKit =>
          val config = EdgeConfigGet.outExample
          val configurator = testKit.spawn(EdgeConfigurator(config))
          val reader = testKit.createTestProbe[EdgeConfig]()
          val newCullingConfig = config.dynamic.culling.copy(maxFirstAgents = 999)

          configurator ! UpdateCullingConfig(newCullingConfig)
          configurator ! Read(reader.ref)

          val receivedConfig = reader.receiveMessage()
          assert(receivedConfig.dynamic.coordinate == config.dynamic.coordinate)
          assert(receivedConfig.dynamic.culling == newCullingConfig)

    describe("UpdateCoordinateConfig"):
      it("distributes coordinate configuration to registered coordinate observers"):
        Using(ActorTestKit()): testKit =>
          val config = EdgeConfigGet.outExample
          val configurator = testKit.spawn(EdgeConfigurator(config))
          val observer1 = testKit.createTestProbe[UpdateCoordinateConfig]()
          val observer2 = testKit.createTestProbe[UpdateCoordinateConfig]()

          testKit.system.receptionist ! Receptionist.register(coordinateObserverKey, observer1.ref)
          testKit.system.receptionist ! Receptionist.register(coordinateObserverKey, observer2.ref)

          assert(observer1.receiveMessage().config == config.dynamic.coordinate)
          assert(observer2.receiveMessage().config == config.dynamic.coordinate)

          val newConfig = config.dynamic.coordinate.copy(rotation = QuaternionConfig)
          configurator ! UpdateCoordinateConfig(newConfig)

          assert(observer1.receiveMessage().config == newConfig)
          assert(observer2.receiveMessage().config == newConfig)

    describe("UpdateCullingConfig"):
      it("distributes culling configuration to registered culling observers"):
        Using(ActorTestKit()): testKit =>
          val config = EdgeConfigGet.outExample
          val configurator = testKit.spawn(EdgeConfigurator(config))
          val observer1 = testKit.createTestProbe[UpdateCullingConfig]()
          val observer2 = testKit.createTestProbe[UpdateCullingConfig]()

          testKit.system.receptionist ! Receptionist.register(cullingObserverKey, observer1.ref)
          testKit.system.receptionist ! Receptionist.register(cullingObserverKey, observer2.ref)

          assert(observer1.receiveMessage() == UpdateCullingConfig(config.dynamic.culling))
          assert(observer2.receiveMessage() == UpdateCullingConfig(config.dynamic.culling))

          val newConfig = config.dynamic.culling.copy(maxFirstAgents = 456)
          configurator ! UpdateCullingConfig(newConfig)

          assert(observer1.receiveMessage() == UpdateCullingConfig(newConfig))
          assert(observer2.receiveMessage() == UpdateCullingConfig(newConfig))
