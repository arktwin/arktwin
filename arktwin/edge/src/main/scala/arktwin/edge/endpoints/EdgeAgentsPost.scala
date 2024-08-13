// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.center.services
import arktwin.center.services.{EdgeAgentsPostRequest, EdgeAgentsPostResponse, RegisterClient}
import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.util.JsonDerivation.given
import arktwin.edge.util.{EdgeKamon, ErrorStatus, RequestValidator}
import cats.implicits.toTraverseOps
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.apache.pekko.http.scaladsl.server.Route
import sttp.model.StatusCode
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.ExecutionContext

object EdgeAgentsPost:
  type Request = EdgeAgentsPostRequest
  type Response = EdgeAgentsPostResponse
  val Request: EdgeAgentsPostRequest.type = EdgeAgentsPostRequest
  val Response: EdgeAgentsPostResponse.type = EdgeAgentsPostResponse
  given codecRequest: JsonValueCodec[Seq[Request]] = JsonCodecMaker.makeWithoutDiscriminator
  given codecResponse: JsonValueCodec[Seq[Response]] = JsonCodecMaker.makeWithoutDiscriminator

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
      endpoint.serverLogic: requests =>
        val requestTime = Timestamp.machineNow()
        RequestValidator(services.EdgeAgentsPostRequests(requests))
          .map(client.edgeAgentsPost)
          .map(_.map(_.responses))
          .sequence
          .recover(ErrorStatus.handleFailure)
          .andThen: _ =>
            requestNumCounter.increment()
            agentNumCounter.increment(requests.size)
            processMachineTimeHistogram.record((Timestamp.machineNow() - requestTime).millisLong)
