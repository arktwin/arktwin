// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.StaticCenterConfig
import arktwin.center.actors.Clock
import arktwin.common.GrpcHeaderKey
import com.google.protobuf.empty.Empty
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.typed.scaladsl.ActorSource
import org.apache.pekko.stream.{Attributes, Materializer, OverflowStrategy}

class ClockService(
    clock: ActorRef[Clock.Message],
    config: StaticCenterConfig
)(using
    Materializer
) extends ClockPowerApi:
  override def subscribe(in: Empty, metadata: Metadata): Source[ClockBase, NotUsed] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.subscribe/$edgeId"

    val (actorRef, source) = ActorSource
      .actorRef[ClockBase](
        PartialFunction.empty,
        PartialFunction.empty,
        config.subscribeBufferSize,
        OverflowStrategy.dropHead
      )
      .log(logName)
      .addAttributes(
        Attributes.logLevels(onFailure = Attributes.LogLevels.Warning, onFinish = Attributes.LogLevels.Warning)
      )
      .preMaterialize()
    clock ! Clock.AddSubscriber(edgeId, actorRef)
    scribe.info(s"[$logName] connected")
    source
