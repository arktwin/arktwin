// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.adapters

import arktwin.center.services.{ChartAgent, ClockBase}
import arktwin.common.data.*
import arktwin.edge.actors.adapters.EdgeAgentsPutAdapter.*
import arktwin.edge.actors.sinks.{Chart, Clock}
import arktwin.edge.configs.Vector3Config.Direction.{East, North, Up}
import arktwin.edge.configs.Vector3Config.LengthUnit.Meter
import arktwin.edge.configs.Vector3Config.TimeUnit.Second
import arktwin.edge.configs.{CoordinateConfig, QuaternionConfig, Vector3Config}
import arktwin.edge.connectors.{ChartConnector, RegisterConnector}
import arktwin.edge.data.*
import arktwin.edge.endpoints.EdgeAgentsPut.{Request, Response}
import arktwin.edge.endpoints.{EdgeAgentsPutRequestAgent, EdgeConfigGet}
import arktwin.edge.test.ActorTestBase
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.collection.mutable

class EdgeAgentsPutAdapterSuite extends ActorTestBase:
  test(EdgeAgentsPutAdapter.getClass.getSimpleName):
    val config = EdgeConfigGet.outExample
    val chart = testKit.createTestProbe[Chart.UpdateFirstAgents]()
    val chartPublish = testKit.createTestProbe[ChartConnector.Publish]()
    val registerPublish = testKit.createTestProbe[RegisterConnector.Publish]()
    val clockReadQueue = mutable.Queue[ClockBase]()
    val clock = testKit.spawn[Clock.Get](Behaviors.receiveMessage:
      case Clock.Get(replyTo) =>
        replyTo ! clockReadQueue.dequeue()
        Behaviors.same)
    val adapter =
      testKit.spawn(
        EdgeAgentsPutAdapter(
          chart.ref,
          chartPublish.ref,
          registerPublish.ref,
          clock.ref,
          config.static,
          config.dynamic.coordinate,
          EdgeKamon("run", "edge")
        )
      )
    val endpoint = testKit.createTestProbe[Either[ErrorStatus, Response]]()

    adapter ! CoordinateConfig(Vector3Config(Meter, Second, East, North, Up), QuaternionConfig)
    clockReadQueue += ClockBase(Timestamp(0, 0), Timestamp(0, 0), 1)
    adapter ! Put(
      Request(
        Some(VirtualTimestamp(1, 0)),
        Map(
          "a" -> EdgeAgentsPutRequestAgent(
            Some(transform(Vector3(1, 2, 3), Some(Vector3(1, 1, 1)))),
            None
          ),
          "b" -> EdgeAgentsPutRequestAgent(Some(transform(Vector3(1, 2, 3), None)), None)
        )
      ),
      MachineTimestamp(0, 0),
      endpoint.ref
    )
    chart.receiveMessage().agents shouldEqual Seq(
      ChartAgent("a", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(1, 1, 1))),
      ChartAgent("b", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0)))
    )
    chartPublish.receiveMessage().agents shouldEqual Seq(
      ChartAgent("a", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(1, 1, 1))),
      ChartAgent("b", transformEnu(Timestamp(1, 0), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0)))
    )

    clockReadQueue += ClockBase(Timestamp(0, 0), Timestamp(0, 0), 1)
    adapter ! Put(
      Request(
        Some(VirtualTimestamp(1, 500_000_000)),
        Map(
          "a" -> EdgeAgentsPutRequestAgent(Some(transform(Vector3(4, 4, 4), None)), None),
          "b" -> EdgeAgentsPutRequestAgent(Some(transform(Vector3(1, 2, 3), None)), None)
        )
      ),
      MachineTimestamp(0, 0),
      endpoint.ref
    )
    chart.receiveMessage().agents shouldEqual Seq(
      ChartAgent(
        "a",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(4, 4, 4), Vector3Enu(6, 4, 2))
      ),
      ChartAgent(
        "b",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0))
      )
    )
    chartPublish.receiveMessage().agents shouldEqual Seq(
      ChartAgent(
        "a",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(4, 4, 4), Vector3Enu(6, 4, 2))
      ),
      ChartAgent(
        "b",
        transformEnu(Timestamp(1, 500_000_000), Vector3Enu(1, 2, 3), Vector3Enu(0, 0, 0))
      )
    )

  private def transform(localTranslation: Vector3, localTranslationSpeed: Option[Vector3]) =
    Transform(
      None,
      Vector3(1, 1, 1),
      Quaternion(1, 0, 0, 0),
      localTranslation,
      localTranslationSpeed,
      None
    )

  private def transformEnu(
      timestamp: Timestamp,
      localTranslation: Vector3Enu,
      localTranslationSpeed: Vector3Enu
  ) =
    TransformEnu(
      timestamp,
      None,
      Vector3Enu(1, 1, 1),
      QuaternionEnu(1, 0, 0, 0),
      localTranslation,
      localTranslationSpeed,
      Map()
    )
