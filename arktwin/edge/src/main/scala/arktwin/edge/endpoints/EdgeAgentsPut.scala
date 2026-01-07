// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.{MachineTimestamp, VirtualTimestamp}
import arktwin.common.util.JsonDerivation
import arktwin.common.util.JsonDerivation.given
import arktwin.edge.actors.adapters.EdgeAgentsPutAdapter
import arktwin.edge.configs.EdgeStaticConfig
import arktwin.edge.data.*
import arktwin.edge.util.EndpointExtensions.serverLogicWithLog
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import sttp.model.StatusCode
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.ExecutionContext

object EdgeAgentsPut:
  type Request = EdgeAgentsPutRequest
  type Response = EdgeAgentsPutResponse
  val Request: EdgeAgentsPutRequest.type = EdgeAgentsPutRequest
  val Response: EdgeAgentsPutResponse.type = EdgeAgentsPutResponse
  given JsonValueCodec[Request] = JsonDerivation.makeCodec
  given JsonValueCodec[Response] = JsonDerivation.makeCodec

  val inExample: Request = Request(
    Some(VirtualTimestamp(123, 100_000_000)),
    Map(
      "alice" -> EdgeAgentsPutRequestAgent(
        Some(
          Transform(
            None,
            Vector3(1, 1, 1),
            EulerAngles(10, 20, 30),
            Vector3(4, 5, 6),
            Some(Vector3(0, 0, 0)),
            Some(Map("heart_rate" -> "80"))
          )
        ),
        Some(Map("emotion" -> "joyful"))
      ),
      "bob" -> EdgeAgentsPutRequestAgent(
        Some(
          Transform(
            None,
            Vector3(1, 1, 1),
            EulerAngles(10, 20, 30),
            Vector3(4, 5, 6),
            None,
            None
          )
        ),
        None
      )
    )
  )

  val outExample: Response = Response(VirtualTimestamp(123, 0))

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.put
      .in("api" / "edge" / "agents")
      .in(jsonBody[Request].example(inExample))
      .out(
        oneOf(
          oneOfVariantValueMatcher(
            StatusCode.Accepted,
            jsonBody[Response].example(outExample)
          )(_ => true)
        )
      )
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.badRequest,
          ErrorStatus.internalServerError,
          ErrorStatus.serviceUnavailable
        )
      )
      .description(
        """The response from this endpoint is not essential for the client, so synchronous calls may cause overhead.
          |Consider using asynchronous calls or the "POST api/edge/_bulk" endpoint instead.""".stripMargin
      )

  def route(
      adapter: ActorRef[EdgeAgentsPutAdapter.Message],
      staticConfig: EdgeStaticConfig,
      kamon: EdgeKamon
  )(using
      ExecutionContext,
      Scheduler
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val agentNumCounter = kamon.restAgentNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    given Timeout = staticConfig.endpointMachineTimeout
    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogicWithLog: request =>
        val requestTime = MachineTimestamp.now()
        adapter
          .?[Either[ErrorStatus, Response]](
            EdgeAgentsPutAdapter.Put(request, MachineTimestamp.now(), _)
          )
          .recover(throwable => Left(ErrorStatus(throwable)))
          .andThen: _ =>
            requestNumCounter.increment()
            agentNumCounter.increment(request.agents.size)
            processMachineTimeHistogram.record(MachineTimestamp.now() - requestTime)

case class EdgeAgentsPutRequest(
    @description("If it is omitted, a current timestamp of the edge virtual clock is used.")
    timestamp: Option[VirtualTimestamp],
    agents: Map[String, EdgeAgentsPutRequestAgent]
)

case class EdgeAgentsPutRequestAgent(
    transform: Option[Transform],
    @description("It should not be updated at high frequency. Partial update is possible.")
    status: Option[Map[String, String]]
)

case class EdgeAgentsPutResponse(
    edgeReceiptTimestamp: VirtualTimestamp
)
