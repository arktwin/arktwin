// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.connectors

import arktwin.center.services.{RegisterAgentUpdated, RegisterAgentsPublish, RegisterClient}
import arktwin.common.GrpcHeaderKey
import arktwin.edge.StaticEdgeConfig
import arktwin.edge.actors.CommonMessages.Nop
import arktwin.edge.actors.sinks.Register
import com.google.protobuf.empty.Empty
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}

object RegisterConnector:
  case class Publish(agents: Seq[RegisterAgentUpdated])

// TODO retry connect in actor?
case class RegisterConnector(client: RegisterClient, staticConfig: StaticEdgeConfig, edgeId: String)(using
    Materializer
):
  import RegisterConnector.*

  def subscribe(register: ActorRef[Register.Message]): Unit =
    // send agents information to Chart individually for quick response edge/neighbors/_query
    client
      .subscribe()
      .addHeader(GrpcHeaderKey.edgeId, edgeId)
      .invoke(Empty())
      .log(getClass.getSimpleName + ".subscribe")
      .mapConcat(_.agents.map(Register.Catch.apply))
      .to(ActorSink.actorRef(register, Nop, _ => Nop))
      .run()

  def publish(): ActorRef[Publish] =
    val (actorRef, source) = ActorSource
      .actorRef[Publish](
        PartialFunction.empty,
        PartialFunction.empty,
        staticConfig.bufferSize,
        OverflowStrategy.dropHead
      )
      .mapConcat: a =>
        a.agents
          .grouped(staticConfig.publishStreamBatchSize)
          .map(RegisterAgentsPublish.apply)
      .preMaterialize()
    client
      .publish()
      .addHeader(GrpcHeaderKey.edgeId, edgeId)
      .invoke(source)
    actorRef
