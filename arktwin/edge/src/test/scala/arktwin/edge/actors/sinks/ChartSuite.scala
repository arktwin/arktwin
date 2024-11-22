// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ChartAgent
import arktwin.common.data.{QuaternionEnu, Timestamp, TransformEnu, Vector3Enu}
import arktwin.edge.actors.sinks.Chart.*
import arktwin.edge.configs.CullingConfig
import arktwin.edge.endpoints.EdgeConfigGet
import arktwin.edge.test.ActorTestBase

class ChartSuite extends ActorTestBase:
  test(Chart.getClass.getSimpleName):
    val config = EdgeConfigGet.outExample
    val chart = testKit.spawn(Chart(config.dynamic.culling))
    val chartReader = testKit.createTestProbe[ReadReply]()

    chart ! CullingConfig(edgeCulling = true, 2)
    chart ! Get(chartReader.ref)
    chartReader.receiveMessage().sortedAgents shouldEqual Seq()

    // test that all received agents can be read
    for i <- 0 to 9
    do chart ! Catch(chartAgent(s"b-$i-$i-$i", Timestamp(0, 0), Vector3Enu(i, i, i)))
    chart ! Get(chartReader.ref)
    chartReader.receiveMessage().sortedAgents.map(_.agent.agentId).toSet shouldEqual
      (0 to 9).map(i => s"b-$i-$i-$i").toSet

    // test culling distance calculation
    chart ! UpdateFirstAgents(
      Seq(
        chartAgent("c0", Timestamp(0, 0), Vector3Enu(1.99, 2.98, 3.97)), // near 2-3-4
        chartAgent("c1", Timestamp(0, 0), Vector3Enu(8.80, 7.81, 6.82)) // near 9-8-7
      )
    )
    for
      x <- 0 to 9
      y <- 0 to 9
      z <- 0 to 9
    do chart ! Catch(chartAgent(s"b-$x-$y-$z", Timestamp(1, 0), Vector3Enu(x, y, z)))
    chart ! Get(chartReader.ref)
    chartReader.receiveMessage().sortedAgents.map(_.agent.agentId).take(9) shouldEqual Seq(
      "b-2-3-4",
      "b-9-8-7",
      "b-8-8-7",
      "b-9-7-7",
      "b-9-8-6",
      "b-2-3-3",
      "b-2-2-4",
      "b-1-3-4",
      "b-3-3-4"
    )

    // test that old information is not received
    for
      x <- 0 to 9
      y <- 0 to 9
      z <- 0 to 9
    do chart ! Catch(chartAgent(s"b-$x-$y-$z", Timestamp(0, 0), Vector3Enu(-x, -y, -z)))
    chart ! Get(chartReader.ref)
    chartReader.receiveMessage().sortedAgents.map(_.agent.agentId).take(1) shouldEqual Seq(
      "b-2-3-4"
    )

    // test that position updates affect culling distance calculation
    for
      x <- 0 to 9
      y <- 0 to 9
      z <- 0 to 9
    do
      chart ! Catch(
        chartAgent(s"b-$x-$y-$z", Timestamp(1, 0), Vector3Enu(x * 0.1, y * 0.1, z * 0.1))
      )
    chart ! Get(chartReader.ref)
    chartReader.receiveMessage().sortedAgents.map(_.agent.agentId).take(3) shouldEqual Seq(
      "b-9-9-9",
      "b-8-9-9",
      "b-9-8-9"
    )

    // test that culling distance calculation is turned off because first agents are many
    chart ! UpdateFirstAgents(
      (0 to 9).map(i => chartAgent(s"c$i", Timestamp(2, 0), Vector3Enu(i, i, i)))
    )
    for
      x <- 0 to 9
      y <- 0 to 9
      z <- 0 to 9
    do
      if !Seq((1, 2, 3), (4, 5, 6), (7, 8, 9)).contains((x, y, z)) then
        chart ! Catch(chartAgent(s"b-$x-$y-$z", Timestamp(2, 0), Vector3Enu(x, y, z)))
    chart ! Get(chartReader.ref)
    chartReader.receiveMessage().sortedAgents.map(_.agent.agentId).take(3) shouldEqual Seq(
      "b-7-8-9",
      "b-4-5-6",
      "b-1-2-3"
    )

  private def chartAgent(
      agentId: String,
      timestamp: Timestamp,
      translation: Vector3Enu
  ): ChartAgent =
    ChartAgent(
      agentId,
      TransformEnu(
        timestamp,
        None,
        Vector3Enu(0, 0, 0),
        QuaternionEnu(0, 0, 0, 0),
        translation,
        Vector3Enu(0, 0, 0),
        Map()
      )
    )
