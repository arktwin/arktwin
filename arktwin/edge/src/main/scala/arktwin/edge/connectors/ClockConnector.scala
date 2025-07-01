// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.connectors

import arktwin.center.services.ClockClient
import arktwin.common.util.CommonMessages.Nop
import arktwin.common.util.GrpcHeaderKey
import arktwin.common.util.SourceExtensions.*
import arktwin.edge.actors.sinks.Clock
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
      .wireTapLog("Clock.Subscribe")
      .map(Clock.Catch.apply)
      .to(ActorSink.actorRef(clock, Nop, _ => Nop))
      .run()
