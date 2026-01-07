// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.actors.Clock
import arktwin.center.configs.CenterStaticConfig
import arktwin.common.util.GrpcHeaderKeys
import arktwin.common.util.SourceExtensions.*
import com.google.protobuf.empty.Empty
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.typed.scaladsl.ActorSource
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.util.Timeout

import scala.concurrent.Future

class ClockService(
    clock: ActorRef[Clock.Message],
    config: CenterStaticConfig
)(using
    Materializer,
    Scheduler,
    Timeout
) extends ClockPowerApi:
  override def read(in: Empty, metadata: Metadata): Future[ClockBase] =
    clock ? (Clock.Read(_))

  override def subscribe(in: Empty, metadata: Metadata): Source[ClockBase, NotUsed] =
    val edgeId = metadata.getText(GrpcHeaderKeys.edgeId).getOrElse("")

    val (actorRef, source) = ActorSource
      .actorRef[ClockBase](
        PartialFunction.empty,
        PartialFunction.empty,
        config.subscribeBufferSize,
        OverflowStrategy.dropHead
      )
      .wireTapLog(s"Clock.Subscribe/$edgeId")
      .preMaterialize()
    clock ! Clock.AddSubscriber(edgeId, actorRef)
    source
