// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.actors.Register
import arktwin.center.config.StaticCenterConfig
import arktwin.center.util.CommonMessages.Nop
import arktwin.common.GrpcHeaderKey
import com.google.protobuf.empty.Empty
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.apache.pekko.stream.{Attributes, Materializer, OverflowStrategy}
import org.apache.pekko.util.Timeout

import scala.concurrent.Future

class RegisterService(
    register: ActorRef[Register.Message],
    config: StaticCenterConfig
)(using
    Materializer,
    Scheduler,
    Timeout
) extends RegisterPowerApi:
  override def createEdge(in: CreateEdgeRequest, metadata: Metadata): Future[CreateEdgeResponse] =
    register ? (Register.CreateEdge(in, _))

  override def createAgents(
      in: CreateAgentsRequest,
      metadata: Metadata
  ): Future[CreateAgentsResponse] =
    register ? (Register.CreateAgents(in, _))

  override def publish(
      in: Source[RegisterAgentsPublish, NotUsed],
      metadata: Metadata
  ): Future[Empty] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.publish/$edgeId"

    in
      .log(logName)
      .addAttributes(
        Attributes.logLevels(
          onFailure = Attributes.LogLevels.Warning,
          onFinish = Attributes.LogLevels.Warning
        )
      )
      .map(Register.UpdateAgents(_))
      .to(ActorSink.actorRef(register, Nop, _ => Nop))
      .run()
    scribe.info(s"[$logName] connected")
    Future.never

  override def subscribe(in: Empty, metadata: Metadata): Source[RegisterAgentsSubscribe, NotUsed] =
    val edgeId = metadata.getText(GrpcHeaderKey.edgeId).getOrElse("")
    val logName = s"${getClass.getSimpleName}.subscribe/$edgeId"

    val (actorRef, source) = ActorSource
      .actorRef[RegisterAgentsSubscribe](
        PartialFunction.empty,
        PartialFunction.empty,
        config.subscribeBufferSize,
        OverflowStrategy.dropHead
      )
      .log(logName)
      .addAttributes(
        Attributes.logLevels(
          onFailure = Attributes.LogLevels.Warning,
          onFinish = Attributes.LogLevels.Warning
        )
      )
      .preMaterialize()
    register ! Register.AddSubscriber(edgeId, actorRef)
    scribe.info(s"[$logName] connected")
    source
