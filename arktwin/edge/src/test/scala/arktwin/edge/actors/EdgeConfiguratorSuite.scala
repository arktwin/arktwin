// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors

import arktwin.edge.DynamicEdgeConfig.CullingConfig
import arktwin.edge.data.{CoordinateConfig, QuaternionConfig}
import arktwin.edge.endpoints.EdgeConfigGet
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.scalatest.funsuite.AnyFunSuiteLike

class EdgeConfiguratorSuite extends ScalaTestWithActorTestKit() with AnyFunSuiteLike:
  import arktwin.edge.actors.EdgeConfigurator.*

  test("EdgeConfigManager"):
    val config = EdgeConfigGet.outExample
    val configurator = testKit.spawn(EdgeConfigurator(config))
    val coordinateObserver = testKit.createTestProbe[CoordinateConfig]()
    val cullingObserver = testKit.createTestProbe[CullingConfig]()

    testKit.system.receptionist ! Receptionist.register(coordinateObserverKey, coordinateObserver.ref)
    coordinateObserver.receiveMessage() shouldEqual config.dynamic.coordinate

    testKit.system.receptionist ! Receptionist.register(cullingObserverKey, cullingObserver.ref)
    cullingObserver.receiveMessage() shouldEqual config.dynamic.culling

    configurator ! config.dynamic.coordinate.copy(rotation = QuaternionConfig)
    coordinateObserver.receiveMessage() shouldEqual config.dynamic.coordinate.copy(rotation = QuaternionConfig)

    configurator ! config.dynamic.culling.copy(maxFirstAgents = 123)
    cullingObserver.receiveMessage() shouldEqual config.dynamic.culling.copy(maxFirstAgents = 123)
