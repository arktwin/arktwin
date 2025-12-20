// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.MachineTimestamp
import arktwin.common.util.JsonDerivation
import arktwin.common.util.JsonDerivation.given
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.actors.EdgeConfigurator.UpdateChartConfig
import arktwin.edge.configs.ChartConfig
import arktwin.edge.util.EndpointExtensions.serverLogicWithLog
import arktwin.edge.util.ErrorStatus.BadRequest
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import cats.data.Validated.{Invalid, Valid}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.http.scaladsl.server.Route
import sttp.model.StatusCode.Accepted
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object EdgeConfigChartPut:
  type Request = ChartConfig
  type Response = Unit
  val Request: ChartConfig.type = ChartConfig
  given JsonValueCodec[Request] = JsonDerivation.makeCodec

  val inExample: Request = Request(
    culling = Request.Culling(
      enabled = true,
      maxFirstAgents = 9
    ),
    expiration = Request.Expiration(
      enabled = false,
      checkMachineInterval = 1.second,
      timeout = 3.seconds
    )
  )

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.put
      .in("api" / "edge" / "config" / "chart")
      .in(jsonBody[Request].example(inExample))
      .out(statusCode(Accepted))
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.badRequest,
          ErrorStatus.internalServerError
        )
      )

  def route(configurator: ActorRef[EdgeConfigurator.Message], kamon: EdgeKamon)(using
      ExecutionContext
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogicWithLog: request =>
        val requestTime = MachineTimestamp.now()
        (request.validated("") match
          case Invalid(errors) =>
            Future.successful(Left(BadRequest(errors.toChain.toVector)))
          case Valid(request) =>
            configurator ! UpdateChartConfig(request)
            Future.successful(Right(()))
        ).andThen: _ =>
          requestNumCounter.increment()
          processMachineTimeHistogram.record(MachineTimestamp.now() - requestTime)
