// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.adapters

import arktwin.center.services.{ChartAgent, ClockBase, RegisterAgent}
import arktwin.common.data.{QuaternionEnu, Timestamp, TransformEnu, Vector3Enu}
import arktwin.edge.actors.adapters.EdgeNeighborsQueryAdapter.*
import arktwin.edge.actors.sinks.Chart.CullingAgent
import arktwin.edge.actors.sinks.{Chart, Clock, Register}
import arktwin.edge.config.EulerAnglesConfig.AngleUnit.Degree
import arktwin.edge.config.EulerAnglesConfig.Order.XYZ
import arktwin.edge.config.Vector3Config.Direction.{East, North, Up}
import arktwin.edge.config.Vector3Config.LengthUnit.Meter
import arktwin.edge.config.Vector3Config.TimeUnit.Second
import arktwin.edge.config.{CoordinateConfig, EulerAnglesConfig, Vector3Config}
import arktwin.edge.data.*
import arktwin.edge.endpoints.EdgeConfigGet
import arktwin.edge.endpoints.EdgeNeighborsQuery.{Request, Response, ResponseAgent}
import arktwin.edge.endpoints.NeighborChange.{Recognized, Unrecognized, Updated}
import arktwin.edge.test.ActorTestBase
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.collection.mutable

class EdgeNeighborsQueryAdapterSuite extends ActorTestBase:
  test(EdgeNeighborsQueryAdapter.getClass.getSimpleName):
    val config = EdgeConfigGet.outExample
    val chartReadQueue = mutable.Queue[Seq[CullingAgent]]()
    val chart = testKit.spawn[Chart.Get](Behaviors.receiveMessage:
      case Chart.Get(replyTo) =>
        replyTo ! Chart.ReadReply(chartReadQueue.dequeue())
        Behaviors.same
    )
    val clockReadQueue = mutable.Queue[ClockBase]()
    val clock = testKit.spawn[Clock.Get](Behaviors.receiveMessage:
      case Clock.Get(replyTo) =>
        replyTo ! clockReadQueue.dequeue()
        Behaviors.same
    )
    val registerReadQueue = mutable.Queue[Map[String, RegisterAgent]]()
    val register = testKit.spawn[Register.Get](Behaviors.receiveMessage:
      case Register.Get(replyTo) =>
        replyTo ! Register.ReadReply(registerReadQueue.dequeue())
        Behaviors.same
    )
    val adapter =
      testKit.spawn(
        EdgeNeighborsQueryAdapter(
          chart,
          clock,
          register,
          config.static,
          config.dynamic.coordinate,
          EdgeKamon("run", "edge")
        )
      )
    val endpoint = testKit.createTestProbe[Either[ErrorStatus, Response]]()

    adapter ! CoordinateConfig(
      Vector3Config(Meter, Second, East, North, Up),
      EulerAnglesConfig(Degree, XYZ)
    )

    chartReadQueue += Seq(
      CullingAgent(ChartAgent("a0", transformEnu()), Some(1)),
      CullingAgent(ChartAgent("a1", transformEnu()), Some(2)),
      CullingAgent(ChartAgent("a2", transformEnu()), Some(3)),
      CullingAgent(ChartAgent("a3", transformEnu()), Some(4)),
      CullingAgent(ChartAgent("a4", transformEnu()), Some(5))
    )
    clockReadQueue += ClockBase(Timestamp(0, 0), Timestamp(0, 0), 1)
    registerReadQueue += Map(
      "a0" -> RegisterAgent("a0", "k0", Map("i" -> "0"), Map("x" -> "x0")),
      "a1" -> RegisterAgent("a1", "k1", Map("i" -> "1"), Map("x" -> "x1")),
      "a2" -> RegisterAgent("a2", "k2", Map("i" -> "2"), Map("x" -> "x2")),
      "a3" -> RegisterAgent("a3", "k3", Map("i" -> "3"), Map("x" -> "x3")),
      "a5" -> RegisterAgent("a5", "k5", Map("i" -> "5"), Map("x" -> "x5"))
    )
    adapter ! Query(
      Request(Some(Timestamp(1, 0)), Some(3), changeDetection = true),
      endpoint.ref
    )
    endpoint.receiveMessage().toOption.get shouldEqual Response(
      Timestamp(1, 0),
      Map(
        "a0" -> ResponseAgent(
          Some(transform()),
          Some(1),
          Some("k0"),
          Some(Map("i" -> "0")),
          Some(Map("x" -> "x0")),
          Some(Recognized)
        ),
        "a1" -> ResponseAgent(
          Some(transform()),
          Some(2),
          Some("k1"),
          Some(Map("i" -> "1")),
          Some(Map("x" -> "x1")),
          Some(Recognized)
        ),
        "a2" -> ResponseAgent(
          Some(transform()),
          Some(3),
          Some("k2"),
          Some(Map("i" -> "2")),
          Some(Map("x" -> "x2")),
          Some(Recognized)
        )
        // "a3" culled

      )
    )

    // check NeighborChange
    chartReadQueue += Seq(
      CullingAgent(ChartAgent("a0", transformEnu()), Some(1)),
      CullingAgent(ChartAgent("a3", transformEnu()), None)
    )
    clockReadQueue += ClockBase(Timestamp(0, 0), Timestamp(0, 0), 1)
    registerReadQueue += Map(
      "a0" -> RegisterAgent("a0", "k0", Map("i" -> "0"), Map("x" -> "x0")),
      "a1" -> RegisterAgent("a1", "k1", Map("i" -> "1"), Map("x" -> "x1")),
      // "a2" deleted
      "a3" -> RegisterAgent("a3", "k3", Map("i" -> "3"), Map("x" -> "x3"))
    )
    adapter ! Query(
      Request(Some(Timestamp(2, 0)), None, changeDetection = true),
      endpoint.ref
    )
    endpoint.receiveMessage().toOption.get shouldEqual Response(
      Timestamp(2, 0),
      Map(
        "a0" -> ResponseAgent(
          Some(transform()),
          Some(1),
          Some("k0"),
          Some(Map("i" -> "0")),
          Some(Map("x" -> "x0")),
          Some(Updated)
        ),
        "a1" -> ResponseAgent(None, None, None, None, None, Some(Unrecognized)),
        "a2" -> ResponseAgent(None, None, None, None, None, Some(Unrecognized)),
        "a3" -> ResponseAgent(
          Some(transform()),
          None,
          Some("k3"),
          Some(Map("i" -> "3")),
          Some(Map("x" -> "x3")),
          Some(Recognized)
        )
      )
    )

  private def transformEnu() = TransformEnu(
    Timestamp(0, 0),
    None,
    Vector3Enu(1, 1, 1),
    QuaternionEnu(1, 0, 0, 0),
    Vector3Enu(0, 0, 0),
    Vector3Enu(0, 0, 0)
  )

  private def transform() = Transform(
    None,
    Vector3(1, 1, 1),
    EulerAngles(180, 0, 0),
    Vector3(0, 0, 0),
    Some(Vector3(0, 0, 0))
  )
