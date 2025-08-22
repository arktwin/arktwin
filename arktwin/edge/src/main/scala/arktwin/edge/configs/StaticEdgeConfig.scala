// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.center.services.CreateEdgeRequest
import arktwin.common.util.LoggerConfigurator.LogLevel
import cats.data.Validated.{condNec, invalidNec, valid}
import cats.data.ValidatedNec
import scalapb.validate.{Failure, Success, Validator}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class StaticEdgeConfig(
    edgeIdPrefix: String,
    host: String,
    port: Int,
    portAutoIncrement: Boolean,
    portAutoIncrementMax: Int,
    logLevel: LogLevel,
    logLevelColor: Boolean,
    logSuppressionList: Seq[String],
    actorMachineTimeout: FiniteDuration,
    endpointMachineTimeout: FiniteDuration,
    clockInitialStashSize: Int,
    publishBatchSize: Int,
    publishBufferSize: Int
):
  def validated(path: String): ValidatedNec[String, StaticEdgeConfig] =
    import cats.syntax.apply.*
    (
      Validator[CreateEdgeRequest].validate(CreateEdgeRequest(edgeIdPrefix)) match
        case Success             => valid(edgeIdPrefix)
        case Failure(violations) =>
          invalidNec(
            s"$path.edgeIdPrefix: " + violations
              .map(_.reason)
              .mkString(", ")
          ),
      condNec(
        host.nonEmpty,
        host,
        s"$path.host must not be empty"
      ),
      condNec(
        port > 0,
        port,
        s"$path.port must be > 0"
      ),
      valid(portAutoIncrement),
      condNec(
        portAutoIncrementMax >= 0,
        portAutoIncrementMax,
        s"$path.portAutoIncrementMax must be >= 0"
      ),
      valid(logLevel),
      valid(logLevelColor),
      valid(logSuppressionList),
      condNec(
        actorMachineTimeout > 0.second,
        actorMachineTimeout,
        s"$path.actorMachineTimeout must be > 0"
      ),
      condNec(
        endpointMachineTimeout > 0.second,
        endpointMachineTimeout,
        s"$path.endpointMachineTimeout must be > 0"
      ),
      condNec(
        clockInitialStashSize > 0,
        clockInitialStashSize,
        s"$path.clockInitialStashSize must be > 0"
      ),
      condNec(
        publishBatchSize > 0,
        publishBatchSize,
        s"$path.publishBatchSize must be > 0"
      ),
      condNec(
        publishBufferSize > 0,
        publishBufferSize,
        s"$path.publishBufferSize must be > 0"
      )
    ).mapN(StaticEdgeConfig.apply)
