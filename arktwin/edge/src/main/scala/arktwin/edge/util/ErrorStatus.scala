// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import arktwin.common.util.JsonDerivation
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import org.slf4j.{Logger, LoggerFactory}
import sttp.model.StatusCode
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.jsonBody
import sttp.tapir.{EndpointOutput, oneOfVariant}

import scala.annotation.tailrec

sealed trait ErrorStatus:
  def message: String

object ErrorStatus:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  case class BadRequest(errors: Seq[String]) extends ErrorStatus:
    override def message: String =
      s"400 Bad Request: ${errors.mkString(", ")}"

  case class InternalServerError(errors: Seq[String]) extends ErrorStatus:
    override def message: String =
      s"500 Internal Server Error: ${errors.mkString(", ")}"

  case class ServiceUnavailable(error: String) extends ErrorStatus:
    override def message: String =
      s"503 Service Unavailable: $error"

  val badRequest: EndpointOutput.OneOfVariant[BadRequest] =
    given JsonValueCodec[BadRequest] = JsonDerivation.makeCodec
    oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequest])

  val internalServerError: EndpointOutput.OneOfVariant[InternalServerError] =
    given JsonValueCodec[InternalServerError] = JsonDerivation.makeCodec
    oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError])

  val serviceUnavailable: EndpointOutput.OneOfVariant[ServiceUnavailable] =
    given JsonValueCodec[ServiceUnavailable] = JsonDerivation.makeCodec
    oneOfVariant(StatusCode.ServiceUnavailable, jsonBody[ServiceUnavailable])

  def apply(throwable: Throwable): ErrorStatus =
    logger.warn("Internal Server Error", throwable)
    InternalServerError(recursiveCauses(List(throwable)).map(_.getMessage).reverse)

  @tailrec
  private def recursiveCauses(ts: List[Throwable]): List[Throwable] =
    val cause = ts.head.getCause
    if cause == null || ts.contains(cause) then ts
    else recursiveCauses(cause +: ts)
