// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.configs.AtlasConfig
import arktwin.center.services.ChartAgent
import arktwin.center.test.ActorTestBase
import arktwin.center.util.CenterKamon
import arktwin.center.util.CommonMessages.Terminate
import arktwin.common.data.*
import arktwin.common.data.TimestampExtensions.*
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}

import java.lang.Thread
import scala.concurrent.duration.DurationInt

class AtlasSpec extends ActorTestBase:
  given Scheduler = testKit.system.scheduler
  given Ordering[ChartAgent] =
    Ordering.by(a => (a.agentId, a.transform.timestamp.tagVirtual))

  describe("Atlas"):
    it("broadcasts messages between multiple publishers and subscribers with Broadcast config"):
      val atlasConfig = AtlasConfig(AtlasConfig.Broadcast(), 100.millis)
      val atlas = testKit.spawn(Atlas(atlasConfig, CenterKamon("")))

      val (publisherA, subscriberA) = createPubSub("A", atlas)
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      val a1 = chartAgent("a1", Timestamp(0, 0), Vector3Enu(1, 2, 3))
      val a2 = chartAgent("a2", Timestamp(0, 0), Vector3Enu(1, 2, 3))
      publisherA ! Chart.PublishBatch(Seq(a1, a2), MachineTimestamp(0, 0))
      subscriberA.expectNoMessage()

      val (publisherB, subscriberB) = createPubSub("B", atlas)
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      val b1 = chartAgent("b1", Timestamp(0, 0), Vector3Enu(1, 2, 3))
      val b2 = chartAgent("b2", Timestamp(0, 0), Vector3Enu(1, 2, 3))
      publisherA ! Chart.PublishBatch(Seq(a1, a2), MachineTimestamp(0, 0))
      publisherB ! Chart.PublishBatch(Seq(b1), MachineTimestamp(0, 0))
      publisherB ! Chart.PublishBatch(Seq(b2), MachineTimestamp(0, 0))
      assert(subscriberA.receiveMessages(2).map(_.agents.sorted) == Seq(Seq(b1), Seq(b2)))
      assert(subscriberB.receiveMessage().agents.sorted == Seq(a1, a2))
      subscriberA.expectNoMessage()
      subscriberB.expectNoMessage()

      val (publisherC, subscriberC) = createPubSub("C", atlas)
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      val c1 = chartAgent("c1", Timestamp(0, 0), Vector3Enu(1, 2, 3))
      val c2 = chartAgent("c2", Timestamp(0, 0), Vector3Enu(1, 2, 3))
      publisherA ! Chart.PublishBatch(Seq(a1, a2), MachineTimestamp(0, 0))
      publisherB ! Chart.PublishBatch(Seq(b1, b2), MachineTimestamp(0, 0))
      publisherC ! Chart.PublishBatch(Seq(c1, c2), MachineTimestamp(0, 0))
      assert(
        subscriberA.receiveMessages(2).map(_.agents.sorted).sortBy(_.head) == Seq(
          Seq(b1, b2),
          Seq(c1, c2)
        )
      )
      assert(
        subscriberB.receiveMessages(2).map(_.agents.sorted).sortBy(_.head) == Seq(
          Seq(a1, a2),
          Seq(c1, c2)
        )
      )
      assert(
        subscriberC.receiveMessages(2).map(_.agents.sorted).sortBy(_.head) == Seq(
          Seq(a1, a2),
          Seq(b1, b2)
        )
      )
      subscriberA.expectNoMessage()
      subscriberB.expectNoMessage()
      subscriberC.expectNoMessage()

      publisherB ! Terminate
      subscriberB.stop()
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      publisherA ! Chart.PublishBatch(Seq(a1, a2), MachineTimestamp(0, 0))
      publisherB ! Chart.PublishBatch(Seq(b1, b2), MachineTimestamp(0, 0))
      publisherC ! Chart.PublishBatch(Seq(c1, c2), MachineTimestamp(0, 0))
      assert(subscriberA.receiveMessage().agents.sorted == Seq(c1, c2))
      assert(subscriberC.receiveMessage().agents.sorted == Seq(a1, a2))
      subscriberA.expectNoMessage()
      subscriberC.expectNoMessage()

    it("filters messages based on grid cells with GridCulling config"):
      val atlasConfig =
        AtlasConfig(AtlasConfig.GridCulling(Vector3Enu(10, 100, 1000)), 100.millis)
      val atlas = testKit.spawn(Atlas(atlasConfig, CenterKamon("")))

      val (publisherA, subscriberA) = createPubSub("A", atlas)
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      val a1_0 = chartAgent("a1", Timestamp(0, 0), Vector3Enu(1, 1, 1)) // [0, 0, 0]
      val a2_0 = chartAgent("a2", Timestamp(0, 0), Vector3Enu(1, 1, 1)) // [0, 0, 0]
      publisherA ! Chart.PublishBatch(Seq(a1_0, a2_0), MachineTimestamp(0, 0))
      subscriberA.expectNoMessage()

      val (publisherB, subscriberB) = createPubSub("B", atlas)
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      val b1_0 = chartAgent("b1", Timestamp(0, 0), Vector3Enu(1, 1, 1)) // [0, 0, 0]
      val b2_0 = chartAgent("b2", Timestamp(0, 0), Vector3Enu(21, 1, 1)) // [2, 0, 0]
      publisherB ! Chart.PublishBatch(Seq(b1_0, b2_0), MachineTimestamp(0, 0))
      assert(subscriberA.receiveMessage().agents.sorted == Seq(b1_0))
      subscriberA.expectNoMessage()
      subscriberB.expectNoMessage()

      val (publisherC, subscriberC) = createPubSub("C", atlas)
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      val c1_0 = chartAgent("c1", Timestamp(0, 0), Vector3Enu(11, 1, 1)) // [1, 0, 0]
      val c2_0 = chartAgent("c2", Timestamp(0, 0), Vector3Enu(31, 1, 1001)) // [3, 0, 1]
      publisherC ! Chart.PublishBatch(Seq(c1_0, c2_0), MachineTimestamp(0, 0))
      assert(subscriberA.receiveMessage().agents.sorted == Seq(c1_0))
      assert(subscriberB.receiveMessage().agents.sorted == Seq(c1_0, c2_0))
      subscriberA.expectNoMessage()
      subscriberB.expectNoMessage()
      subscriberC.expectNoMessage()

      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      val a1_1 = chartAgent("a1", Timestamp(1, 0), Vector3Enu(41, 101, 1001)) // [4, 1, 1]
      val a2_1 = chartAgent("a2", Timestamp(1, 0), Vector3Enu(41, 201, -1)) // [4, 2, -1]
      publisherA ! Chart.PublishBatch(Seq(a1_1, a2_1), MachineTimestamp(0, 0))
      assert(subscriberC.receiveMessage().agents.sorted == Seq(a1_1))
      subscriberA.expectNoMessage()
      subscriberB.expectNoMessage()
      subscriberC.expectNoMessage()

      publisherB ! Terminate
      subscriberB.stop()
      Thread.sleep(atlasConfig.routeTableUpdateInterval.toMillis * 2)

      publisherC ! Chart.PublishBatch(Seq(c1_0, c2_0), MachineTimestamp(0, 0))
      assert(subscriberA.receiveMessage().agents.sorted == Seq(c2_0))
      subscriberA.expectNoMessage()
      subscriberC.expectNoMessage()

  private def createPubSub(
      edgeId: String,
      atlas: ActorRef[Atlas.Message]
  )(using Scheduler) =
    val chart =
      (atlas ? (Atlas.SpawnChart(edgeId, _: ActorRef[ActorRef[Chart.Message]]))).futureValue
    val chartRecorder = (atlas ? (Atlas.SpawnChartRecorder(
      edgeId,
      _: ActorRef[ActorRef[ChartRecorder.Message]]
    ))).futureValue
    val chartPublisher = testKit.spawn[Chart.PublishBatch | Terminate.type](
      Behaviors.receiveMessage:
        case publishBatch: Chart.PublishBatch =>
          chart ! publishBatch
          chartRecorder ! publishBatch
          Behaviors.same
        case Terminate =>
          chart ! Terminate
          chartRecorder ! Terminate
          Behaviors.stopped
    )
    val chartSubscriber = testKit.createTestProbe[Chart.SubscribeBatch]()
    atlas ! Atlas.AddChartSubscriber(edgeId, chartSubscriber.ref)
    (chartPublisher, chartSubscriber)

  private def chartAgent(
      agentId: String,
      timestamp: Timestamp,
      localTranslation: Vector3Enu
  ) =
    ChartAgent(
      agentId,
      TransformEnu(
        timestamp,
        None,
        Vector3Enu(1, 1, 1),
        QuaternionEnu(1, 0, 0, 0),
        localTranslation,
        Vector3Enu(0, 0, 0),
        Map()
      )
    )
