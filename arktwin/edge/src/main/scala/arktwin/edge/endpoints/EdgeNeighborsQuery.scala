// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.StaticEdgeConfig
import arktwin.edge.actors.adapters.EdgeNeighborsQueryAdapter
import arktwin.edge.data.*
import arktwin.edge.util.JsonDerivation.given
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.ExecutionContext

object EdgeNeighborsQuery:
  type Request = EdgeNeighborsQueryRequest
  type Response = EdgeNeighborsQueryResponse
  type ResponseAgent = EdgeNeighborsQueryResponseAgent
  val Request: EdgeNeighborsQueryRequest.type = EdgeNeighborsQueryRequest
  val Response: EdgeNeighborsQueryResponse.type = EdgeNeighborsQueryResponse
  val ResponseAgent: EdgeNeighborsQueryResponseAgent.type = EdgeNeighborsQueryResponseAgent
  given codecRequest: JsonValueCodec[Request] = JsonCodecMaker.makeWithoutDiscriminator
  given codecResponse: JsonValueCodec[Response] = JsonCodecMaker.make(
    CodecMakerConfig
      .withDiscriminatorFieldName(None)
      .withRequireCollectionFields(true)
      .withTransientEmpty(false)
      .withMapMaxInsertNumber(Int.MaxValue)
  )

  val inExample: Request =
    Request(Some(Timestamp(234, 100_000_000)), Some(100), changeDetection = true)

  val outExample: Response = Response(
    Timestamp(234, 100_000_000),
    Map(
      "alice" -> ResponseAgent(
        Some(
          Transform(
            None,
            Vector3(1, 1, 1),
            EulerAngles(10, 20, 30),
            Vector3(4, 5, 6),
            Some(Vector3(0, 0, 0))
          )
        ),
        Some(1.23),
        Some("human"),
        Some(Map("emotion" -> "joyful")),
        Some(Map("model" -> "girl.obj")),
        Some(NeighborChange.Updated)
      )
    )
  )

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.post
      .description("alternative to QUERY method")
      .in("api" / "edge" / "neighbors" / "_query")
      .in(jsonBody[Request].example(inExample))
      .out(jsonBody[Response].example(outExample))
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.badRequest,
          ErrorStatus.internalServerError,
          ErrorStatus.serviceUnavailable
        )
      )

  def route(
      adapter: ActorRef[EdgeNeighborsQueryAdapter.Message],
      staticConfig: StaticEdgeConfig,
      kamon: EdgeKamon
  )(using
      ExecutionContext,
      Scheduler
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    given Timeout = staticConfig.endpointTimeout
    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogic: request =>
        val requestTime = Timestamp.machineNow()
        adapter
          .?[Either[ErrorStatus, Response]](EdgeNeighborsQueryAdapter.Query(request, _))
          .recover(ErrorStatus.handleFailure)
          .andThen: _ =>
            requestNumCounter.increment()
            processMachineTimeHistogram.record((Timestamp.machineNow() - requestTime).millisLong)

case class EdgeNeighborsQueryRequest(
    @description("If it is omitted, a current timestamp of the edge simulation clock is used.")
    timestamp: Option[Timestamp],
    @description("If it is omitted, this API returns all neighbors.")
    neighborsNumber: Option[Int],
    @description("If it is true, this API is locked for a moment to update previous neighbors.")
    changeDetection: Boolean
)

enum NeighborChange:
  case Recognized, Updated, Unrecognized
object NeighborChange:
  given Schema[NeighborChange] = Schema
    .derivedEnumeration[NeighborChange](encode = Some(_.toString))
    .description(
      """- Recognized: This agent is previously unrecognized, currently recognized.
        |- Updated: This agent is previously recognized, currently recognized.
        |- Unrecognized: This agent is previously recognized, currently unrecognized.
        |""".stripMargin
    )

case class EdgeNeighborsQueryResponse(
    timestamp: Timestamp,
    neighbors: Map[String, EdgeNeighborsQueryResponseAgent]
)

case class EdgeNeighborsQueryResponseAgent(
    transform: Option[Transform],
    nearestDistance: Option[Double],
    kind: Option[String],
    status: Option[Map[String, String]],
    assets: Option[Map[String, String]],
    change: Option[NeighborChange]
)
