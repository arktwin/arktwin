// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.MachineTimestamp
import arktwin.common.util.JsonDerivation
import arktwin.common.util.JsonDerivation.given
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.actors.EdgeConfigurator.UpdateCoordinateConfig
import arktwin.edge.configs.AxisConfig.Direction
import arktwin.edge.configs.CoordinateConfig.{LengthUnit, SpeedUnit}
import arktwin.edge.configs.EulerAnglesConfig.{AngleUnit, RotationMode, RotationOrder}
import arktwin.edge.configs.{AxisConfig, CoordinateConfig, EulerAnglesConfig}
import arktwin.edge.data.Vector3
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

import scala.concurrent.{ExecutionContext, Future}

object EdgeConfigCoordinatePut:
  type Request = CoordinateConfig
  type Response = Unit
  val Request: CoordinateConfig.type = CoordinateConfig
  given JsonValueCodec[Request] = JsonDerivation.makeCodec

  val inExample: Request = Request(
    axis = AxisConfig(
      xDirection = Direction.East,
      yDirection = Direction.North,
      zDirection = Direction.Up
    ),
    centerOrigin = Vector3(0, 0, 0),
    rotation = EulerAnglesConfig(
      angleUnit = AngleUnit.Degree,
      rotationMode = RotationMode.Extrinsic,
      rotationOrder = RotationOrder.XYZ
    ),
    lengthUnit = LengthUnit.Meter,
    speedUnit = SpeedUnit.MeterPerSecond
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
      endpoint.serverLogicWithLog: request =>
        val requestTime = MachineTimestamp.now()
        (request.validated("") match
          case Invalid(errors) =>
            Future.successful(Left(BadRequest(errors.toChain.toVector)))
          case Valid(request) =>
            configurator ! UpdateCoordinateConfig(request)
            Future.successful(Right(()))
        ).andThen: _ =>
          requestNumCounter.increment()
          processMachineTimeHistogram.record(MachineTimestamp.now() - requestTime)
