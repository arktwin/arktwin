// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.center.services.{AdminClient, UpdateClockSpeedRequest}
import arktwin.common.data.MachineTimestamp
import arktwin.common.util.JsonDerivation
import arktwin.common.util.JsonDerivation.given
import arktwin.edge.util.EndpointExtensions.serverLogicWithLog
import arktwin.edge.util.{EdgeKamon, ErrorStatus, RequestValidator}
import cats.implicits.toTraverseOps
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import org.apache.pekko.http.scaladsl.server.Route
import sttp.model.StatusCode.Accepted
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.ExecutionContext

object CenterClockSpeedPut:
  type Request = UpdateClockSpeedRequest
  type Response = Unit
  val Request: UpdateClockSpeedRequest.type = UpdateClockSpeedRequest
  given JsonValueCodec[Request] = JsonDerivation.makeCodec

  val inExample: Request = Request(1.2)

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.put
      .in("api" / "center" / "clock" / "speed")
      .in(jsonBody[Request].example(inExample))
      .out(statusCode(Accepted))
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.badRequest,
          ErrorStatus.internalServerError
        )
      )

  def route(client: AdminClient, kamon: EdgeKamon)(using
      ExecutionContext
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogicWithLog: request =>
        val requestTime = MachineTimestamp.now()
        RequestValidator(request)
          .map(client.updateClockSpeed(_).map(_ => {}))
          .sequence
          .recover(throwable => Left(ErrorStatus(throwable)))
          .andThen: _ =>
            requestNumCounter.increment()
            processMachineTimeHistogram.record(MachineTimestamp.now() - requestTime)
