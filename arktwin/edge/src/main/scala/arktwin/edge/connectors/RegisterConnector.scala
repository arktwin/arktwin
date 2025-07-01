// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.connectors

import arktwin.center.services.{RegisterAgentUpdated, RegisterAgentsPublish, RegisterClient}
import arktwin.common.util.GrpcHeaderKey
import arktwin.common.util.SourceExtensions.*
import arktwin.edge.actors.sinks.Register
import arktwin.edge.configs.StaticEdgeConfig
import arktwin.edge.util.CommonMessages.Nop
import com.google.protobuf.empty.Empty
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}

// TODO retry connect in actor?
case class RegisterConnector(
    client: RegisterClient,
    staticConfig: StaticEdgeConfig,
    edgeId: String
)(using
    Materializer
):
  import RegisterConnector.*

  def publish(): ActorRef[Publish] =
    val (actorRef, source) = ActorSource
      .actorRef[Publish](
        PartialFunction.empty,
        PartialFunction.empty,
        staticConfig.publishBufferSize,
        OverflowStrategy.dropHead
      )
      .mapConcat: a =>
        a.agents
          .grouped(staticConfig.publishBatchSize)
          .map(RegisterAgentsPublish.apply)
      .wireTapLog("Register.Publish")
      .preMaterialize()
    client
      .publish()
      .addHeader(GrpcHeaderKey.edgeId, edgeId)
      .invoke(source)
    actorRef

  def subscribe(register: ActorRef[Register.Message]): Unit =
    // send agents information to Chart individually for quick response edge/neighbors/_query
    client
      .subscribe()
      .addHeader(GrpcHeaderKey.edgeId, edgeId)
      .invoke(Empty())
      .wireTapLog("Register.Subscribe")
      .mapConcat(_.agents.map(Register.Catch.apply))
      .to(ActorSink.actorRef(register, Nop, _ => Nop))
      .run()

object RegisterConnector:
  case class Publish(agents: Seq[RegisterAgentUpdated])
