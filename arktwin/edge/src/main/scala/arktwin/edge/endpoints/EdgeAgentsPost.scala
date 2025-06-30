// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.center.services
import arktwin.center.services.{CreateAgentRequest, CreateAgentResponse, RegisterClient}
import arktwin.common.data.TaggedTimestamp
import arktwin.common.util.JsonDerivation
import arktwin.common.util.JsonDerivation.given
import arktwin.edge.util.EndpointExtensions.serverLogicWithLog
import arktwin.edge.util.{EdgeKamon, ErrorStatus, RequestValidator}
import cats.implicits.toTraverseOps
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import org.apache.pekko.http.scaladsl.server.Route
import sttp.model.StatusCode
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.ExecutionContext

object EdgeAgentsPost:
  type Request = CreateAgentRequest
  type Response = CreateAgentResponse
  val Request: CreateAgentRequest.type = CreateAgentRequest
  val Response: CreateAgentResponse.type = CreateAgentResponse
  given codecRequest: JsonValueCodec[Seq[Request]] = JsonDerivation.makeCodec
  given codecResponse: JsonValueCodec[Seq[Response]] = JsonDerivation.makeCodec

  val inExample: Seq[Request] = Seq(
    Request("alice", "human", Map("emotion" -> "relaxed"), Map("model" -> "girl.obj")),
    Request("humpty-dumpty", "egg", Map("emotion" -> "relaxed"), Map("model" -> "big_egg.obj"))
  )

  val outExample: Seq[Response] = Seq(
    Response("alice-i"),
    Response("humpty-dumpty-3")
  )

  val endpoint: PublicEndpoint[Seq[Request], ErrorStatus, Seq[Response], Any] =
    tapir.endpoint.post
      .in("api" / "edge" / "agents")
      .in(jsonBody[Seq[Request]].example(inExample))
      .out(
        oneOf(
          oneOfVariantValueMatcher(
            StatusCode.Created,
            jsonBody[Seq[Response]].example(outExample)
          )(_ => true)
        )
      )
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.badRequest,
          ErrorStatus.internalServerError
        )
      )

  def route(client: RegisterClient, kamon: EdgeKamon)(using
      ExecutionContext
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val agentNumCounter = kamon.restAgentNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogicWithLog: requests =>
        val requestTime = TaggedTimestamp.machineNow()
        RequestValidator(services.CreateAgentsRequest(requests))
          .map(client.createAgents)
          .map(_.map(_.responses))
          .sequence
          .recover(throwable => Left(ErrorStatus(throwable)))
          .andThen: _ =>
            requestNumCounter.increment()
            agentNumCounter.increment(requests.size)
            processMachineTimeHistogram.record(TaggedTimestamp.machineNow() - requestTime)
