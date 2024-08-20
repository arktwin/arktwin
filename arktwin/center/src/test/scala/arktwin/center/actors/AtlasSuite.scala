// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.actors

import arktwin.center.AtlasConfig
import arktwin.center.services.ChartAgent
import arktwin.center.test.ActorTestBase
import arktwin.center.util.CenterKamon
import arktwin.center.util.CommonMessages.Terminate
import arktwin.common.data.TimestampEx.given
import arktwin.common.data.{QuaternionEnu, Timestamp, TransformEnu, Vector3Enu}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}

import java.lang.Thread
import scala.concurrent.duration.DurationInt

class AtlasSuite extends ActorTestBase:
  given Scheduler = testKit.system.scheduler
  given Ordering[ChartAgent] = Ordering.by(a => (a.agentId, a.transform.timestamp))

  test("broadcast"):
    val atlasConfig = AtlasConfig(AtlasConfig.Broadcast(), 100.millis)
    val atlas = testKit.spawn(Atlas(atlasConfig, CenterKamon("")))

    val (publisherA, subscriberA) = createPubSub("A", atlas)
    val a1 = ChartAgent("a1", transformEnu(Timestamp(0, 0), Vector3Enu(1, 2, 3)))
    val a2 = ChartAgent("a2", transformEnu(Timestamp(0, 0), Vector3Enu(1, 2, 3)))
    Thread.sleep(atlasConfig.interval.toMillis * 2)

    publisherA ! Chart.PublishBatch(Seq(a1, a2), Timestamp(0, 0))
    subscriberA.expectNoMessage()

    val (publisherB, subscriberB) = createPubSub("B", atlas)
    val b1 = ChartAgent("b1", transformEnu(Timestamp(0, 0), Vector3Enu(1, 2, 3)))
    val b2 = ChartAgent("b2", transformEnu(Timestamp(0, 0), Vector3Enu(1, 2, 3)))
    Thread.sleep(atlasConfig.interval.toMillis * 2)

    publisherA ! Chart.PublishBatch(Seq(a1, a2), Timestamp(0, 0))
    publisherB ! Chart.PublishBatch(Seq(b1), Timestamp(0, 0))
    publisherB ! Chart.PublishBatch(Seq(b2), Timestamp(0, 0))
    subscriberA.receiveMessages(2).map(_.agents.sorted) shouldEqual Seq(Seq(b1), Seq(b2))
    subscriberB.receiveMessage().agents.sorted shouldEqual Seq(a1, a2)
    subscriberA.expectNoMessage()
    subscriberB.expectNoMessage()

    val (publisherC, subscriberC) = createPubSub("C", atlas)
    val c1 = ChartAgent("c1", transformEnu(Timestamp(0, 0), Vector3Enu(1, 2, 3)))
    val c2 = ChartAgent("c2", transformEnu(Timestamp(0, 0), Vector3Enu(1, 2, 3)))
    Thread.sleep(atlasConfig.interval.toMillis * 2)

    publisherA ! Chart.PublishBatch(Seq(a1, a2), Timestamp(0, 0))
    publisherB ! Chart.PublishBatch(Seq(b1, b2), Timestamp(0, 0))
    publisherC ! Chart.PublishBatch(Seq(c1, c2), Timestamp(0, 0))
    subscriberA.receiveMessages(2).map(_.agents.sorted).sortBy(_.head) shouldEqual Seq(
      Seq(b1, b2),
      Seq(c1, c2)
    )
    subscriberB.receiveMessages(2).map(_.agents.sorted).sortBy(_.head) shouldEqual Seq(
      Seq(a1, a2),
      Seq(c1, c2)
    )
    subscriberC.receiveMessages(2).map(_.agents.sorted).sortBy(_.head) shouldEqual Seq(
      Seq(a1, a2),
      Seq(b1, b2)
    )
    subscriberA.expectNoMessage()
    subscriberB.expectNoMessage()
    subscriberC.expectNoMessage()

    publisherB ! Terminate
    subscriberB.stop()
    Thread.sleep(atlasConfig.interval.toMillis * 2)

    publisherA ! Chart.PublishBatch(Seq(a1, a2), Timestamp(0, 0))
    publisherB ! Chart.PublishBatch(Seq(b1, b2), Timestamp(0, 0))
    publisherC ! Chart.PublishBatch(Seq(c1, c2), Timestamp(0, 0))
    subscriberA.receiveMessage().agents.sorted shouldEqual Seq(c1, c2)
    subscriberC.receiveMessage().agents.sorted shouldEqual Seq(a1, a2)
    subscriberA.expectNoMessage()
    subscriberC.expectNoMessage()

  // TODO test("grid culling"):

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

  private def transformEnu(
      timestamp: Timestamp,
      localTranslation: Vector3Enu
  ) =
    TransformEnu(
      timestamp,
      None,
      Vector3Enu(1, 1, 1),
      QuaternionEnu(1, 0, 0, 0),
      localTranslation,
      Vector3Enu(0, 0, 0)
    )
