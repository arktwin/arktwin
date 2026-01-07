// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.center.actors.Register
import arktwin.center.configs.CenterStaticConfig
import arktwin.common.util.CommonMessages.Nop
import arktwin.common.util.GrpcHeaderKeys
import arktwin.common.util.SourceExtensions.*
import com.google.protobuf.empty.Empty
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.grpc.scaladsl.Metadata
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.util.Timeout

import scala.concurrent.Future

class RegisterService(
    register: ActorRef[Register.Message],
    config: CenterStaticConfig
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
    val edgeId = metadata.getText(GrpcHeaderKeys.edgeId).getOrElse("")

    in
      .wireTapLog(s"Register.Publish/$edgeId")
      .map(Register.UpdateAgents(_))
      .to(ActorSink.actorRef(register, Nop, _ => Nop))
      .run()
    Future.never

  override def subscribe(in: Empty, metadata: Metadata): Source[RegisterAgentsSubscribe, NotUsed] =
    val edgeId = metadata.getText(GrpcHeaderKeys.edgeId).getOrElse("")

    val (actorRef, source) = ActorSource
      .actorRef[RegisterAgentsSubscribe](
        PartialFunction.empty,
        PartialFunction.empty,
        config.subscribeBufferSize,
        OverflowStrategy.dropHead
      )
      .wireTapLog(s"Register.Subscribe/$edgeId")
      .preMaterialize()
    register ! Register.AddSubscriber(edgeId, actorRef)
    source
