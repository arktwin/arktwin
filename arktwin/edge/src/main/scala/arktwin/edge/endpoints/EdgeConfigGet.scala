// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*
import arktwin.common.LoggerConfigurator.LogLevel
import arktwin.edge.{DynamicEdgeConfig, EdgeConfig, StaticEdgeConfig}
import arktwin.edge.actors.EdgeConfigurator
import arktwin.edge.util.EdgeKamon
import arktwin.edge.util.ErrorStatus
import arktwin.edge.util.JsonDerivation.given
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorRef, Scheduler}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import sttp.tapir
import sttp.tapir.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object EdgeConfigGet:
  type Request = Unit
  type Response = EdgeConfig
  val Response: EdgeConfig.type = EdgeConfig
  given JsonValueCodec[Response] = JsonCodecMaker.makeWithoutDiscriminator

  val outExample: Response = Response(
    dynamic = DynamicEdgeConfig(
      coordinate = EdgeConfigCoordinatePut.inExample,
      culling = EdgeConfigCullingPut.inExample
    ),
    static = StaticEdgeConfig(
      actorTimeout = 90.milliseconds,
      bufferSize = 10000,
      edgeIdPrefix = "edge",
      endpointTimeout = 100.milliseconds,
      host = "0.0.0.0",
      port = 2237,
      logLevel = LogLevel.Info,
      publishStreamBatchSize = 100
    )
  )

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.get
      .in("api" / "edge" / "config")
      .out(jsonBody[Response].example(outExample))
      .errorOut(
        oneOf[ErrorStatus](
          ErrorStatus.internalServerError
        )
      )

  def route(configurator: ActorRef[EdgeConfigurator.Message], staticConfig: StaticEdgeConfig, kamon: EdgeKamon)(using
      ExecutionContext,
      Scheduler
  ): Route =
    val requestNumCounter = kamon.restRequestNumCounter(endpoint.showShort)
    val processMachineTimeHistogram = kamon.restProcessMachineTimeHistogram(endpoint.showShort)

    given Timeout = staticConfig.endpointTimeout
    PekkoHttpServerInterpreter().toRoute:
      endpoint.serverLogic: _ =>
        val requestTime = Timestamp.machineNow()
        (configurator ? EdgeConfigurator.Get.apply)
          .map(Right[ErrorStatus, EdgeConfig].apply)
          .recover(ErrorStatus.handleFailure)
          .andThen: _ =>
            requestNumCounter.increment()
            processMachineTimeHistogram.record((Timestamp.machineNow() - requestTime).millisLong)
