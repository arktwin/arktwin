// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.endpoints

import arktwin.center.services.*
import arktwin.common.data.TaggedTimestamp
import arktwin.common.data.TimestampEx.*
import arktwin.edge.util.JsonDerivation.given
import arktwin.edge.util.{EdgeKamon, ErrorStatus, JsonDerivation, JsonValidator, RequestValidator}
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

object CenterAgentsDelete:
  type Request = AgentSelector
  type Response = Unit
  given JsonValueCodec[Request] = JsonCodecMaker.makeWithoutDiscriminator
  given Schema[AgentIdSelector] =
    Schema.derived[AgentIdSelector].modify(_.regex)(JsonValidator.regex)
  given Schema[AgentKindSelector] =
    Schema.derived[AgentKindSelector].modify(_.regex)(JsonValidator.regex)
  given Schema[AgentSelector] =
    JsonDerivation.fixCoproductSchemaWithoutDiscriminator(Schema.derived)

  val inExample: Request = AgentIdSelector("alice-.+")

  val endpoint: PublicEndpoint[Request, ErrorStatus, Response, Any] =
    tapir.endpoint.delete
      .in("api" / "center" / "agents")
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
      endpoint.serverLogic: request =>
        val requestTime = TaggedTimestamp.machineNow()
        RequestValidator(request.asMessage)
          .map(client.deleteAgents(_).map(_ => {}))
          .sequence
          .recover(ErrorStatus.handleFailure)
          .andThen: _ =>
            requestNumCounter.increment()
            processMachineTimeHistogram.record(TaggedTimestamp.machineNow() - requestTime)
