// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.actors.adapters.EdgeAgentsPutAdapter
import arktwin.edge.config.StaticEdgeConfig
import arktwin.edge.data.*
import arktwin.edge.util.JsonDerivation.given
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
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
  given codecRequest: JsonValueCodec[Request] = JsonCodecMaker.make(
    CodecMakerConfig
      .withDiscriminatorFieldName(None)
      .withRequireCollectionFields(true)
      .withTransientEmpty(false)
      .withMapMaxInsertNumber(Int.MaxValue)
  )
  given codecResponse: JsonValueCodec[Response] = JsonCodecMaker.makeWithoutDiscriminator

  val inExample: Request = Request(
    Some(Timestamp(123, 100_000_000)),
    Map(
      "alice" -> EdgeAgentsPutRequestAgent(
        Some(
          Transform(
            None,
            Vector3(1, 1, 1),
            EulerAngles(10, 20, 30),
            Vector3(4, 5, 6),
            Some(Vector3(0, 0, 0))
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
            Some(Vector3(0, 0, 0))
          )
        ),
        None
      )
    )
  )

  val outExample: Response = Response(Timestamp(123, 0))

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

  def route(
      adapter: ActorRef[EdgeAgentsPutAdapter.Message],
      staticConfig: StaticEdgeConfig,
      kamon: EdgeKamon
  )(using
      ExecutionContext,
      Scheduler
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val agentNumCounter = kamon.restAgentNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    given Timeout = staticConfig.endpointTimeout
    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogic: request =>
        val requestTime = Timestamp.machineNow()
        adapter
          .?[Either[ErrorStatus, Response]](
            EdgeAgentsPutAdapter.Put(request, Timestamp.machineNow(), _)
          )
          .recover(ErrorStatus.handleFailure)
          .andThen: _ =>
            requestNumCounter.increment()
            agentNumCounter.increment(request.agents.size)
            processMachineTimeHistogram.record((Timestamp.machineNow() - requestTime).millisLong)

case class EdgeAgentsPutRequest(
    @description("If it is omitted, a current timestamp of the edge simulation clock is used.")
    timestamp: Option[Timestamp],
    agents: Map[String, EdgeAgentsPutRequestAgent]
)

case class EdgeAgentsPutRequestAgent(
    transform: Option[Transform],
    @description("It is not expected to be updated at high frequency. Partial update is possible.")
    status: Option[Map[String, String]]
)

case class EdgeAgentsPutResponse(
    edgeReceiptTimestamp: Timestamp
)
