// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import org.slf4j.{Logger, LoggerFactory}
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.{EndpointOutput, oneOfVariant}

import scala.annotation.tailrec

sealed trait ErrorStatus
case class BadRequest(errors: Seq[String]) extends ErrorStatus
case class InternalServerError(errors: Seq[String]) extends ErrorStatus
case class ServiceUnavailable(error: String) extends ErrorStatus

object ErrorStatus:
  val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  val badRequest: EndpointOutput.OneOfVariant[BadRequest] =
    given JsonValueCodec[BadRequest] = JsonCodecMaker.makeWithoutDiscriminator
    oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequest])

  val internalServerError: EndpointOutput.OneOfVariant[InternalServerError] =
    given JsonValueCodec[InternalServerError] = JsonCodecMaker.makeWithoutDiscriminator
    oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError])

  val serviceUnavailable: EndpointOutput.OneOfVariant[ServiceUnavailable] =
    given JsonValueCodec[ServiceUnavailable] = JsonCodecMaker.makeWithoutDiscriminator
    oneOfVariant(StatusCode.ServiceUnavailable, jsonBody[ServiceUnavailable])

  def handleFailure[T]: PartialFunction[Throwable, Either[ErrorStatus, T]] =
    case throwable =>
      logger.warn("Internal Server Error", throwable)
      Left(InternalServerError(recursiveCauses(List(throwable)).map(_.getMessage).reverse))

  @tailrec
  private def recursiveCauses(ts: List[Throwable]): List[Throwable] =
    val cause = ts.head.getCause
    if cause == null || ts.contains(cause) then ts
    else recursiveCauses(cause +: ts)
