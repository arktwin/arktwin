// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.StaticCenterConfig
import arktwin.center.actors.Clock
import arktwin.common.GrpcHeaderKey
import com.google.protobuf.empty.Empty
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.typed.scaladsl.ActorSource
import org.apache.pekko.stream.{Attributes, Materializer, OverflowStrategy}
import org.slf4j.Logger

class ClockService(
    receptionist: ActorRef[Receptionist.Command],
    config: StaticCenterConfig,
    log: Logger
)(using
    Materializer
) extends ClockPowerApi:
  override def subscribe(in: Empty, metadata: Metadata): Source[ClockBase, NotUsed] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.subscribe/$edgeId"

    val (actorRef, source) = ActorSource
      .actorRef[ClockBase](PartialFunction.empty, PartialFunction.empty, config.bufferSize, OverflowStrategy.dropHead)
      .log(logName)
      .addAttributes(
        Attributes.logLevels(onFailure = Attributes.LogLevels.Warning, onFinish = Attributes.LogLevels.Warning)
      )
      .preMaterialize()
    receptionist ! Receptionist.Register(Clock.subscriberKey, actorRef)
    log.info(s"[$logName] connected")
    source
