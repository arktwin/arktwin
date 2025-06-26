// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.TaggedTimestamp
import arktwin.edge.actors.adapters.{EdgeAgentsPutAdapter, EdgeNeighborsQueryAdapter}
import arktwin.edge.configs.StaticEdgeConfig
import arktwin.edge.data.*
import arktwin.edge.util.EndpointExtensions.serverLogicWithLog
import arktwin.edge.util.ErrorStatus.InternalServerError
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

import scala.concurrent.{ExecutionContext, Future}

object EdgeBulk:
  type Request = EdgeBulkRequest
  type Response = EdgeBulkResponse
  type ResponsePart[A] = EdgeBulkResponsePart[A]
  val Request: EdgeBulkRequest.type = EdgeBulkRequest
  val Response: EdgeBulkResponse.type = EdgeBulkResponse
  val ResponsePart: EdgeBulkResponsePart.type = EdgeBulkResponsePart
  given JsonValueCodec[Request] = JsonCodecMaker.make(
    CodecMakerConfig
      .withDiscriminatorFieldName(None)
      .withRequireCollectionFields(true)
      .withTransientEmpty(false)
      .withMapMaxInsertNumber(Int.MaxValue)
  )
  given JsonValueCodec[Response] = JsonCodecMaker.make(
    CodecMakerConfig
      .withDiscriminatorFieldName(None)
      .withRequireCollectionFields(true)
      .withTransientEmpty(false)
      .withMapMaxInsertNumber(Int.MaxValue)
  )

  val inExample: Request = Request(
    Some(EdgeAgentsPut.inExample),
    Some(EdgeNeighborsQuery.inExample)
  )

  val outExample: Response = Response(
    Some(ResponsePart(EdgeAgentsPut.outExample)),
    Some(ResponsePart(InternalServerError(Seq("unexpected error"))))
  )

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.post
      .in("api" / "edge" / "_bulk")
      .in(jsonBody[Request].example(inExample))
      .out(jsonBody[Response].example(outExample))
      .description(
        """Bulk endpoint to reduce network overhead by combining following endpoints:
          |  - PUT /api/edge/agents
          |  - POST /api/edge/neighbors/_query""".stripMargin
      )
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.badRequest
        )
      )

  def route(
      agentsPutAdapter: ActorRef[EdgeAgentsPutAdapter.Message],
      neighborsQueryAdapter: ActorRef[EdgeNeighborsQueryAdapter.Message],
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
      endpoint.serverLogicWithLog: request =>
        val requestTime = TaggedTimestamp.machineNow()

        val agentsPutFuture = request.agentsPut match
          case Some(agentsRequest) =>
            agentsPutAdapter
              .?[Either[ErrorStatus, EdgeAgentsPutResponse]](
                EdgeAgentsPutAdapter.Put(agentsRequest, TaggedTimestamp.machineNow(), _)
              )
              .map:
                _.fold(
                  error => Some(ResponsePart(error)),
                  response => Some(ResponsePart(response))
                )
              .recover(throwable => Some(ResponsePart(ErrorStatus(throwable))))
          case None => Future.successful(None)

        val neighborsQueryFuture = request.neighborsQuery match
          case Some(neighborsRequest) =>
            neighborsQueryAdapter
              .?[Either[ErrorStatus, EdgeNeighborsQueryResponse]](
                EdgeNeighborsQueryAdapter.Query(neighborsRequest, _)
              )
              .map:
                _.fold(
                  error => Some(ResponsePart(error)),
                  response => Some(ResponsePart(response))
                )
              .recover(throwable => Some(ResponsePart(ErrorStatus(throwable))))
          case None => Future.successful(None)

        val responseFuture = for
          agentsResult <- agentsPutFuture
          neighborsResult <- neighborsQueryFuture
        yield Right(Response(agentsResult, neighborsResult))

        responseFuture.andThen: _ =>
          requestNumCounter.increment()
          processMachineTimeHistogram.record(TaggedTimestamp.machineNow() - requestTime)

case class EdgeBulkRequest(
    @description("Same as the request for PUT /api/edge/agents")
    agentsPut: Option[EdgeAgentsPutRequest],
    @description("Same as the request for POST /api/edge/neighbors/_query")
    neighborsQuery: Option[EdgeNeighborsQueryRequest]
)

case class EdgeBulkResponse(
    @description("Same as the response for PUT /api/edge/agents")
    agentsPut: Option[EdgeBulkResponsePart[EdgeAgentsPutResponse]],
    @description("Same as the response for POST /api/edge/neighbors/_query")
    neighborsQuery: Option[EdgeBulkResponsePart[EdgeNeighborsQueryResponse]]
)

case class EdgeBulkResponsePart[A](success: Option[A], error: Option[String])

object EdgeBulkResponsePart:
  def apply[A](success: A): EdgeBulkResponsePart[A] =
    EdgeBulkResponsePart(Some(success), None)

  def apply[A](error: ErrorStatus): EdgeBulkResponsePart[A] =
    EdgeBulkResponsePart(None, Some(error.message))
