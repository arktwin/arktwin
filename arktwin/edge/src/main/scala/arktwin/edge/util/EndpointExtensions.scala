// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.util;

import sttp.tapir.PublicEndpoint
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object EndpointExtensions:
  extension [Input, Output](endpoint: PublicEndpoint[Input, ErrorStatus, Output, Any])
    def serverLogicWithLog(
        f: Input => Future[Either[ErrorStatus, Output]]
    )(using
        ExecutionContext
    ): ServerEndpoint.Full[Unit, Unit, Input, ErrorStatus, Output, Any, Future] =
      endpoint.serverLogic: request =>
        scribe.trace(s"[${endpoint.showShort}] Request: $request")
        f(request).andThen:
          case Success(Right(response)) =>
            scribe.trace(s"[${endpoint.showShort}] Response: $response")
          case Success(Left(errorStatus)) =>
            scribe.warn(s"[${endpoint.showShort}] Response: $errorStatus")
          case Failure(throwable) =>
            scribe.warn(s"[${endpoint.showShort}] Error: $throwable")
