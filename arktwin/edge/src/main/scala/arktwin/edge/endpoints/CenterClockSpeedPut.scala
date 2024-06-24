/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.edge.endpoints

import arktwin.center.services.{AdminClient, CenterClockSpeedPutRequest}
import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.util.EdgeKamon
import arktwin.edge.util.JsonDerivation.given
import arktwin.edge.util.{ErrorStatus, RequestValidator}
import cats.implicits.toTraverseOps
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.apache.pekko.http.scaladsl.server.Route
import sttp.model.StatusCode.Accepted
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.ExecutionContext

object CenterClockSpeedPut:
  type Request = CenterClockSpeedPutRequest
  type Response = Unit
  val Request: CenterClockSpeedPutRequest.type = CenterClockSpeedPutRequest
  given JsonValueCodec[Request] = JsonCodecMaker.makeWithoutDiscriminator

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

  def route(client: AdminClient, edgeId: String)(using
      ExecutionContext
  ): Route =
    val requestNumCounter = EdgeKamon.restRequestNumCounter(edgeId, endpoint.showShort)
    val processMachineTimeHistogram = EdgeKamon.restProcessMachineTimeHistogram(edgeId, endpoint.showShort)

    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogic: request =>
        val requestTime = Timestamp.machineNow()
        RequestValidator(request)
          .map(client.centerClockSpeedPut(_).map(_ => {}))
          .sequence
          .recover(ErrorStatus.handleFailure)
          .andThen: _ =>
            requestNumCounter.increment()
            processMachineTimeHistogram.record((Timestamp.machineNow() - requestTime).millisLong)
