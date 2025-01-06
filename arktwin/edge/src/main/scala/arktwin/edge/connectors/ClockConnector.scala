// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.connectors

import arktwin.center.services.ClockClient
import arktwin.common.GrpcHeaderKey
import arktwin.edge.actors.sinks.Clock
import arktwin.edge.util.CommonMessages.Nop
import com.google.protobuf.empty.Empty
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.typed.scaladsl.ActorSink

// TODO retry connection in actor?
case class ClockConnector(client: ClockClient, edgeId: String)(using Materializer):
  def subscribe(clock: ActorRef[Clock.Message]): Unit =
    client
      .subscribe()
      .addHeader(GrpcHeaderKey.edgeId, edgeId)
      .invoke(Empty())
      .log(getClass.getSimpleName + ".subscribe")
      .map(Clock.Catch.apply)
      .to(ActorSink.actorRef(clock, Nop, _ => Nop))
      .run()
