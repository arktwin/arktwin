// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.ChartAgent
import arktwin.common.data.*
import arktwin.edge.actors.EdgeConfigurator.UpdateChartConfig
import arktwin.edge.actors.sinks.Chart.*
import arktwin.edge.configs.ChartConfig
import arktwin.edge.test.ActorTestBase
import org.scalactic.{Equality, TolerantNumerics}

class ChartSpec extends ActorTestBase:
  private given Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.001)

  private given Equality[Option[Double]] =
    case (Some(a), Some(b)) =>
      summon[Equality[Double]].areEqual(a, b)
    case (None, None) =>
      true
    case _ =>
      false

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

  describe("Chart"):
    describe("Read"):
      it("replies empty sequence when no agents are registered"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = true, maxFirstAgents = 1)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! Read(reader.ref)

        val orderedNeighbors = reader.receiveMessage().orderedNeighbors
        assert(orderedNeighbors == Seq())

    describe("UpdateChartConfig"):
      it("updates chart configuration dynamically"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = false, maxFirstAgents = 0)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! UpdateFirstAgents(Seq(chartAgent("first-1", Timestamp(0, 0), Vector3Enu(0, 0, 0))))
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(0, 0), Vector3Enu(3, 4, 0)))
        chart ! Read(reader.ref)
        {
          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1)
          assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
          assert(orderedNeighbors(0).nearestDistance == None)
        }

        chart ! UpdateChartConfig(ChartConfig(edgeCulling = true, maxFirstAgents = 10))
        chart ! UpdateFirstAgents(Seq(chartAgent("first-1", Timestamp(0, 0), Vector3Enu(0, 0, 0))))
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(1, 0), Vector3Enu(3, 4, 0)))
        chart ! Read(reader.ref)
        {
          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1)
          assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
          assert(orderedNeighbors(0).nearestDistance === Some(5.0))
        }

    describe("UpdateFirstAgents"):
      it("updates first agents when culling is enabled"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = true, maxFirstAgents = 10)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! UpdateFirstAgents(
          Seq(
            chartAgent("first-1", Timestamp(0, 0), Vector3Enu(0, 0, 0)),
            chartAgent("first-2", Timestamp(0, 0), Vector3Enu(5, 5, 5))
          )
        )
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(0, 0), Vector3Enu(1, 0, 0)))
        chart ! Read(reader.ref)

        val orderedNeighbors = reader.receiveMessage().orderedNeighbors
        assert(orderedNeighbors.size == 1)
        assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
        assert(orderedNeighbors(0).nearestDistance === Some(1.0)) // (0,0,0) to (1,0,0)

      it("ignores first agents when culling is disabled"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = false, maxFirstAgents = 0)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! UpdateFirstAgents(
          Seq(
            chartAgent("first-1", Timestamp(0, 0), Vector3Enu(0, 0, 0)),
            chartAgent("first-2", Timestamp(0, 0), Vector3Enu(5, 5, 5))
          )
        )
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(0, 0), Vector3Enu(1, 0, 0)))
        chart ! Read(reader.ref)

        val orderedNeighbors = reader.receiveMessage().orderedNeighbors
        assert(orderedNeighbors.size == 1)
        assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
        assert(orderedNeighbors(0).nearestDistance == None)

      it("ignores first agents when exceeding maxFirstAgents"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = true, maxFirstAgents = 2)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! UpdateFirstAgents(
          Seq(
            chartAgent("first-1", Timestamp(0, 0), Vector3Enu(1, 1, 1))
          )
        )
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(0, 0), Vector3Enu(1, 0, 0)))
        chart ! Read(reader.ref)
        {
          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1)
          assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
          assert(orderedNeighbors(0).nearestDistance == Some(math.sqrt(2)))
        }

        chart ! UpdateFirstAgents(
          Seq(
            chartAgent("first-1", Timestamp(0, 0), Vector3Enu(1, 1, 1)),
            chartAgent("first-2", Timestamp(0, 0), Vector3Enu(2, 2, 2)),
            chartAgent("first-3", Timestamp(0, 0), Vector3Enu(3, 3, 3))
          )
        )
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(0, 1), Vector3Enu(1, 0, 0)))
        chart ! Read(reader.ref)
        {
          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1)
          assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
          assert(orderedNeighbors(0).nearestDistance == None)
        }

    describe("UpdateNeighbor"):
      it("adds new agents"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = true, maxFirstAgents = 1)))
        val reader = testKit.createTestProbe[ReadReply]()

        for i <- 1 to 3 do
          chart ! UpdateNeighbor(chartAgent(s"agent-$i", Timestamp(0, 0), Vector3Enu(i, i, i)))
        chart ! Read(reader.ref)

        val orderedNeighbors = reader.receiveMessage().orderedNeighbors
        assert(
          orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1", "agent-2", "agent-3")
        )
        assert(orderedNeighbors.forall(_.nearestDistance == None))

      it("updates existing agents with newer timestamp"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = true, maxFirstAgents = 1)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(0, 0), Vector3Enu(1, 1, 1)))
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(1, 0), Vector3Enu(2, 2, 2)))
        chart ! Read(reader.ref)

        val orderedNeighbors = reader.receiveMessage().orderedNeighbors
        assert(orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1"))
        assert(orderedNeighbors(0).neighbor.transform.timestamp == Timestamp(1, 0))
        assert(orderedNeighbors(0).neighbor.transform.localTranslationMeter == Vector3Enu(2, 2, 2))
        assert(orderedNeighbors(0).nearestDistance == None)

      it("ignores existing agents with older timestamp"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = true, maxFirstAgents = 1)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(2, 0), Vector3Enu(2, 2, 2)))
        chart ! UpdateNeighbor(chartAgent("agent-1", Timestamp(1, 0), Vector3Enu(1, 1, 1)))
        chart ! Read(reader.ref)

        val orderedNeighbors = reader.receiveMessage().orderedNeighbors
        assert(orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1"))
        assert(orderedNeighbors(0).neighbor.transform.localTranslationMeter == Vector3Enu(2, 2, 2))
        assert(orderedNeighbors(0).neighbor.transform.timestamp == Timestamp(2, 0))
        assert(orderedNeighbors(0).nearestDistance == None)

      it("sorts neighbors by distance from nearest first agent"):
        val chart = testKit.spawn(Chart(ChartConfig(edgeCulling = true, maxFirstAgents = 10)))
        val reader = testKit.createTestProbe[ReadReply]()

        chart ! UpdateFirstAgents(
          Seq(
            chartAgent("a0", Timestamp(0, 0), Vector3Enu(1.99, 2.98, 3.97)), // near b-2-3-4
            chartAgent("a1", Timestamp(0, 0), Vector3Enu(8.80, 7.81, 6.82)) // near b-9-8-7
          )
        )
        for
          x <- 0 to 9
          y <- 0 to 9
          z <- 0 to 9
        do chart ! UpdateNeighbor(chartAgent(s"b-$x-$y-$z", Timestamp(1, 0), Vector3Enu(x, y, z)))
        chart ! Read(reader.ref)
        {
          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1000)
          assert(
            orderedNeighbors.map(_.neighbor.agentId).take(9) == Seq(
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
          )
          assert(
            orderedNeighbors(0).nearestDistance === Some(
              math.sqrt(0.0014)
            ) // (1.99, 2.98, 3.97) to (2, 3, 4)
          )
          assert(
            orderedNeighbors(1).nearestDistance === Some(
              math.sqrt(0.1085)
            ) // (8.80, 7.81, 6.82) to (9, 8, 7)
          )
          assert(
            orderedNeighbors(2).nearestDistance === Some(
              math.sqrt(0.7085)
            ) // (8.80, 7.81, 6.82) to (8, 8, 7)
          )
        }

        for
          x <- 0 to 9
          y <- 0 to 9
          z <- 0 to 9
        do
          chart ! UpdateNeighbor(
            chartAgent(s"b-$x-$y-$z", Timestamp(1, 0), Vector3Enu(x * 0.1, y * 0.1, z * 0.1))
          )
        chart ! Read(reader.ref)
        {
          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1000)
          assert(
            orderedNeighbors.map(_.neighbor.agentId).take(3) == Seq(
              "b-9-9-9",
              "b-8-9-9",
              "b-9-8-9"
            )
          )
        }
