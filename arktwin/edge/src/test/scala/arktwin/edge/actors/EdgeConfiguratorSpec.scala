// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors

import arktwin.edge.actors.EdgeConfigurator.*
import arktwin.edge.configs.{CoordinateConfig, CullingConfig, QuaternionConfig}
import arktwin.edge.endpoints.EdgeConfigGet
import arktwin.edge.test.ActorTestBase
import org.apache.pekko.actor.typed.receptionist.Receptionist

class EdgeConfiguratorSpec extends ActorTestBase:
  describe("EdgeConfigurator"):
    it("notifies observers when they register or when configuration changes"):
      val config = EdgeConfigGet.outExample
      val configurator = testKit.spawn(EdgeConfigurator(config))
      val coordinateObserver = testKit.createTestProbe[CoordinateConfig]()
      val cullingObserver = testKit.createTestProbe[CullingConfig]()

      testKit.system.receptionist ! Receptionist.register(
        coordinateObserverKey,
        coordinateObserver.ref
      )
      assert(coordinateObserver.receiveMessage() == config.dynamic.coordinate)

      testKit.system.receptionist ! Receptionist.register(cullingObserverKey, cullingObserver.ref)
      assert(cullingObserver.receiveMessage() == config.dynamic.culling)

      configurator ! config.dynamic.coordinate.copy(rotation = QuaternionConfig)
      assert(
        coordinateObserver.receiveMessage() == config.dynamic.coordinate
          .copy(rotation = QuaternionConfig)
      )

      configurator ! config.dynamic.culling.copy(maxFirstAgents = 123)
      assert(cullingObserver.receiveMessage() == config.dynamic.culling.copy(maxFirstAgents = 123))
