// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.DynamicEdgeConfig.CullingConfig
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.util.JsonDerivation.given
import arktwin.edge.util.{EdgeKamon, ErrorStatus}
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.http.scaladsl.server.Route
import sttp.model.StatusCode.Accepted
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.{ExecutionContext, Future}

object EdgeConfigCullingPut:
  type Request = CullingConfig
  type Response = Unit
  val Request: CullingConfig.type = CullingConfig
  given JsonValueCodec[Request] = JsonCodecMaker.makeWithoutDiscriminator

  val inExample: Request = Request(edgeCulling = true, 9)

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.put
      .in("api" / "edge" / "config" / "culling")
      .in(jsonBody[Request].example(inExample))
      .out(statusCode(Accepted))
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.internalServerError
        )
      )

  def route(configurator: ActorRef[EdgeConfigurator.Message], kamon: EdgeKamon)(using
      ExecutionContext
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogic: request =>
        val requestTime = Timestamp.machineNow()
        configurator ! request
        Future
          .successful(Right(()): Either[ErrorStatus, Response])
          .andThen: _ =>
            requestNumCounter.increment()
            processMachineTimeHistogram.record((Timestamp.machineNow() - requestTime).millisLong)
