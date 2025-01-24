// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.TaggedTimestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.configs.AxisConfig.Direction
import arktwin.edge.configs.CoordinateConfig.{LengthUnit, SpeedUnit}
import arktwin.edge.configs.EulerAnglesConfig.{AngleUnit, RotationOrder}
import arktwin.edge.configs.{AxisConfig, CoordinateConfig, EulerAnglesConfig}
import arktwin.edge.data.Vector3
import arktwin.edge.util.JsonDerivation.given
import arktwin.edge.util.{BadRequest, EdgeKamon, ErrorStatus}
import cats.data.Validated.{Invalid, Valid}
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

object EdgeConfigCoordinatePut:
  type Request = CoordinateConfig
  type Response = Unit
  val Request: CoordinateConfig.type = CoordinateConfig
  given JsonValueCodec[Request] = JsonCodecMaker.makeWithoutDiscriminator

  val inExample: Request = Request(
    Vector3(0, 0, 0),
    AxisConfig(
      Direction.East,
      Direction.North,
      Direction.Up
    ),
    EulerAnglesConfig(
      AngleUnit.Degree,
      RotationOrder.XYZ
    ),
    LengthUnit.Meter,
    SpeedUnit.MeterPerSecond
  )

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.put
      .in("api" / "edge" / "config" / "coordinate")
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
      endpoint.serverLogic: request =>
        val requestTime = TaggedTimestamp.machineNow()
        (request.validated("") match
          case Invalid(errors) =>
            Future.successful(Left(BadRequest(errors.toChain.toVector)))
          case Valid(request) =>
            configurator ! request
            Future.successful(Right(()): Either[ErrorStatus, Response])
        ).andThen: _ =>
          requestNumCounter.increment()
          processMachineTimeHistogram.record(TaggedTimestamp.machineNow() - requestTime)
