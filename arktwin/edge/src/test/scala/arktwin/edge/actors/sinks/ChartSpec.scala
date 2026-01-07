// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.{ChartAgent, ClockBase}
import arktwin.common.data.*
import arktwin.edge.actors.EdgeConfigurator.UpdateChartConfig
import arktwin.edge.actors.sinks.Chart.*
import arktwin.edge.actors.sinks.Clock.UpdateClockBase
import arktwin.edge.configs.ChartConfig
import arktwin.edge.test.ActorTestBase
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalactic.{Equality, TolerantNumerics}

import scala.concurrent.duration.DurationInt
import scala.util.Using

class ChartSpec extends ActorTestBase:
  private given Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.001)

  private given Equality[Option[Double]] =
    case (Some(a), Some(b)) =>
      summon[Equality[Double]].areEqual(a, b)
    case (None, None) =>
      true
    case _ =>
      false

  private val initClockBase = ClockBase(MachineTimestamp(1, 2), VirtualTimestamp(3, 4), 5.6)

  private val baseConfig = ChartConfig(
    culling = ChartConfig.Culling(
      enabled = true,
      maxFirstAgents = 9
    ),
    expiration = ChartConfig.Expiration(
      enabled = false,
      checkMachineInterval = 1.second,
      timeout = 3.seconds
    )
  )

  private def chartAgent(
      agentId: String,
      timestamp: VirtualTimestamp,
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
    describe("DeleteExpiredNeighbors"):
      it("deletes expired neighbors when expiration is enabled"):
        Using(ActorTestKit()): testKit =>
          val expirationConfig = baseConfig.copy(
            expiration = ChartConfig.Expiration(
              enabled = true,
              checkMachineInterval = 100.milliseconds,
              timeout = 2.seconds
            )
          )
          val initClockBase = ClockBase(
            MachineTimestamp.now(),
            VirtualTimestamp(0, 0),
            1.0
          )
          val chart = testKit.spawn(Chart(initClockBase, expirationConfig))
          val clock = testKit.spawn(Clock(initClockBase))
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 0), Vector3Enu(1, 1, 1)))
          chart ! UpdateNeighbor(chartAgent("agent-2", VirtualTimestamp(0, 0), Vector3Enu(2, 2, 2)))
          chart ! UpdateNeighbor(chartAgent("agent-3", VirtualTimestamp(0, 0), Vector3Enu(3, 3, 3)))
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(
              orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1", "agent-2", "agent-3")
            )
          }

          clock ! UpdateClockBase(
            ClockBase(
              MachineTimestamp.now(),
              VirtualTimestamp(10, 0),
              1.0
            )
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(7, 0), Vector3Enu(1, 1, 1)))
          chart ! UpdateNeighbor(chartAgent("agent-2", VirtualTimestamp(9, 0), Vector3Enu(2, 2, 2)))
          chart ! UpdateNeighbor(
            chartAgent("agent-3", VirtualTimestamp(10, 0), Vector3Enu(3, 3, 3))
          )
          Thread.sleep(expirationConfig.expiration.checkMachineInterval.toMillis * 2)
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(
              orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-2", "agent-3")
            )
          }

      it("does not delete neighbors when expiration is disabled"):
        Using(ActorTestKit()): testKit =>
          val expirationConfig = baseConfig.copy(
            expiration = ChartConfig.Expiration(
              enabled = false,
              checkMachineInterval = 100.milliseconds,
              timeout = 2.seconds
            )
          )
          val initClockBase = ClockBase(
            MachineTimestamp.now(),
            VirtualTimestamp(0, 0),
            1.0
          )
          val chart = testKit.spawn(Chart(initClockBase, expirationConfig))
          val clock = testKit.spawn(Clock(initClockBase))
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 0), Vector3Enu(1, 1, 1)))
          chart ! UpdateNeighbor(chartAgent("agent-2", VirtualTimestamp(0, 0), Vector3Enu(2, 2, 2)))
          chart ! UpdateNeighbor(chartAgent("agent-3", VirtualTimestamp(0, 0), Vector3Enu(3, 3, 3)))
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(
              orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1", "agent-2", "agent-3")
            )
          }

          clock ! UpdateClockBase(
            ClockBase(
              MachineTimestamp.now(),
              VirtualTimestamp(10, 0),
              1.0
            )
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(7, 0), Vector3Enu(1, 1, 1)))
          chart ! UpdateNeighbor(chartAgent("agent-2", VirtualTimestamp(9, 0), Vector3Enu(2, 2, 2)))
          chart ! UpdateNeighbor(
            chartAgent("agent-3", VirtualTimestamp(10, 0), Vector3Enu(3, 3, 3))
          )
          Thread.sleep(expirationConfig.expiration.checkMachineInterval.toMillis * 2)
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(
              orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1", "agent-2", "agent-3")
            )
          }

    describe("Read"):
      it("replies empty sequence when no agents are registered"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(Chart(initClockBase, baseConfig))
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! Read(reader.ref)

          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors == Seq())

    describe("UpdateChartConfig"):
      it("updates chart culling configuration dynamically"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(
            Chart(
              initClockBase,
              baseConfig.copy(culling = ChartConfig.Culling(enabled = false, maxFirstAgents = 0))
            )
          )
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateFirstAgents(
            Seq(chartAgent("first-1", VirtualTimestamp(0, 0), Vector3Enu(0, 0, 0)))
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 0), Vector3Enu(3, 4, 0)))
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(orderedNeighbors.size == 1)
            assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
            assert(orderedNeighbors(0).nearestDistance == None)
          }

          chart ! UpdateChartConfig(
            baseConfig.copy(culling = ChartConfig.Culling(enabled = true, maxFirstAgents = 10))
          )
          chart ! UpdateFirstAgents(
            Seq(chartAgent("first-1", VirtualTimestamp(0, 0), Vector3Enu(0, 0, 0)))
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(1, 0), Vector3Enu(3, 4, 0)))
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(orderedNeighbors.size == 1)
            assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
            assert(orderedNeighbors(0).nearestDistance === Some(5.0))
          }

      it("updates chart expiration configuration dynamically"):
        Using(ActorTestKit()): testKit =>
          val expirationConfig = baseConfig.copy(
            expiration = ChartConfig.Expiration(
              enabled = false,
              checkMachineInterval = 100.milliseconds,
              timeout = 1.day
            )
          )
          val initClockBase = ClockBase(
            MachineTimestamp.now(),
            VirtualTimestamp(123, 0),
            1.0
          )
          val chart = testKit.spawn(Chart(initClockBase, expirationConfig))
          val reader = testKit.createTestProbe[ReadReply]()
          chart ! UpdateNeighbor(
            chartAgent("agent-1", VirtualTimestamp(122, 0), Vector3Enu(1, 1, 1))
          )
          chart ! UpdateNeighbor(
            chartAgent("agent-2", VirtualTimestamp(121, 0), Vector3Enu(2, 2, 2))
          )
          chart ! UpdateNeighbor(
            chartAgent("agent-3", VirtualTimestamp(100, 0), Vector3Enu(3, 3, 3))
          )
          Thread.sleep(expirationConfig.expiration.checkMachineInterval.toMillis * 2)
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(
              orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1", "agent-2", "agent-3")
            )
          }

          val newExpirationConfig = baseConfig.copy(
            expiration = ChartConfig.Expiration(
              enabled = true,
              checkMachineInterval = 100.milliseconds,
              timeout = 10.seconds
            )
          )
          chart ! UpdateChartConfig(newExpirationConfig)
          Thread.sleep(newExpirationConfig.expiration.checkMachineInterval.toMillis * 2)
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(
              orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1", "agent-2")
            )
          }

    describe("UpdateFirstAgents"):
      it("updates first agents when culling is enabled"):
        Using(ActorTestKit()): testKit =>
          val chart =
            testKit.spawn(
              Chart(
                initClockBase,
                baseConfig.copy(culling = ChartConfig.Culling(enabled = true, maxFirstAgents = 10))
              )
            )
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateFirstAgents(
            Seq(
              chartAgent("first-1", VirtualTimestamp(0, 0), Vector3Enu(0, 0, 0)),
              chartAgent("first-2", VirtualTimestamp(0, 0), Vector3Enu(5, 5, 5))
            )
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 0), Vector3Enu(1, 0, 0)))
          chart ! Read(reader.ref)

          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1)
          assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
          assert(orderedNeighbors(0).nearestDistance === Some(1.0)) // (0,0,0) to (1,0,0)

      it("ignores first agents when culling is disabled"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(
            Chart(
              initClockBase,
              baseConfig.copy(culling = ChartConfig.Culling(enabled = false, maxFirstAgents = 0))
            )
          )
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateFirstAgents(
            Seq(
              chartAgent("first-1", VirtualTimestamp(0, 0), Vector3Enu(0, 0, 0)),
              chartAgent("first-2", VirtualTimestamp(0, 0), Vector3Enu(5, 5, 5))
            )
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 0), Vector3Enu(1, 0, 0)))
          chart ! Read(reader.ref)

          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.size == 1)
          assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
          assert(orderedNeighbors(0).nearestDistance == None)

      it("ignores first agents when exceeding cullingMaxFirstAgents"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(
            Chart(
              initClockBase,
              baseConfig.copy(culling = ChartConfig.Culling(enabled = true, maxFirstAgents = 2))
            )
          )
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateFirstAgents(
            Seq(
              chartAgent("first-1", VirtualTimestamp(0, 0), Vector3Enu(1, 1, 1))
            )
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 0), Vector3Enu(1, 0, 0)))
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(orderedNeighbors.size == 1)
            assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
            assert(orderedNeighbors(0).nearestDistance == Some(math.sqrt(2)))
          }

          chart ! UpdateFirstAgents(
            Seq(
              chartAgent("first-1", VirtualTimestamp(0, 0), Vector3Enu(1, 1, 1)),
              chartAgent("first-2", VirtualTimestamp(0, 0), Vector3Enu(2, 2, 2)),
              chartAgent("first-3", VirtualTimestamp(0, 0), Vector3Enu(3, 3, 3))
            )
          )
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 1), Vector3Enu(1, 0, 0)))
          chart ! Read(reader.ref)
          {
            val orderedNeighbors = reader.receiveMessage().orderedNeighbors
            assert(orderedNeighbors.size == 1)
            assert(orderedNeighbors(0).neighbor.agentId == "agent-1")
            assert(orderedNeighbors(0).nearestDistance == None)
          }

    describe("UpdateNeighbor"):
      it("adds new agents"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(Chart(initClockBase, baseConfig))
          val reader = testKit.createTestProbe[ReadReply]()

          for i <- 1 to 3 do
            chart ! UpdateNeighbor(
              chartAgent(s"agent-$i", VirtualTimestamp(0, 0), Vector3Enu(i, i, i))
            )
          chart ! Read(reader.ref)

          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(
            orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1", "agent-2", "agent-3")
          )
          assert(orderedNeighbors.forall(_.nearestDistance == None))

      it("updates existing agents with newer timestamp"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(Chart(initClockBase, baseConfig))
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(0, 0), Vector3Enu(1, 1, 1)))
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(1, 0), Vector3Enu(2, 2, 2)))
          chart ! Read(reader.ref)

          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1"))
          assert(orderedNeighbors(0).neighbor.transform.timestamp == VirtualTimestamp(1, 0))
          assert(
            orderedNeighbors(0).neighbor.transform.localTranslationMeter == Vector3Enu(2, 2, 2)
          )
          assert(orderedNeighbors(0).nearestDistance == None)

      it("ignores existing agents with older timestamp"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(Chart(initClockBase, baseConfig))
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(2, 0), Vector3Enu(2, 2, 2)))
          chart ! UpdateNeighbor(chartAgent("agent-1", VirtualTimestamp(1, 0), Vector3Enu(1, 1, 1)))
          chart ! Read(reader.ref)

          val orderedNeighbors = reader.receiveMessage().orderedNeighbors
          assert(orderedNeighbors.map(_.neighbor.agentId).toSet == Set("agent-1"))
          assert(
            orderedNeighbors(0).neighbor.transform.localTranslationMeter == Vector3Enu(2, 2, 2)
          )
          assert(orderedNeighbors(0).neighbor.transform.timestamp == VirtualTimestamp(2, 0))
          assert(orderedNeighbors(0).nearestDistance == None)

      it("sorts neighbors by distance from nearest first agent"):
        Using(ActorTestKit()): testKit =>
          val chart = testKit.spawn(Chart(initClockBase, baseConfig))
          val reader = testKit.createTestProbe[ReadReply]()

          chart ! UpdateFirstAgents(
            Seq(
              chartAgent(
                "a0",
                VirtualTimestamp(0, 0),
                Vector3Enu(1.99, 2.98, 3.97)
              ), // near b-2-3-4
              chartAgent("a1", VirtualTimestamp(0, 0), Vector3Enu(8.80, 7.81, 6.82)) // near b-9-8-7
            )
          )
          for
            x <- 0 to 9
            y <- 0 to 9
            z <- 0 to 9
          do
            chart ! UpdateNeighbor(
              chartAgent(s"b-$x-$y-$z", VirtualTimestamp(1, 0), Vector3Enu(x, y, z))
            )
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
              chartAgent(
                s"b-$x-$y-$z",
                VirtualTimestamp(1, 0),
                Vector3Enu(x * 0.1, y * 0.1, z * 0.1)
              )
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
