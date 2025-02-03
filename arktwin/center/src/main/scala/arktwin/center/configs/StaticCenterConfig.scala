// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.LoggerConfigurator.LogLevel
import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class StaticCenterConfig(
    clock: ClockConfig,
    runIdPrefix: String,
    host: String,
    port: Int,
    portAutoIncrement: Boolean,
    portAutoIncrementMax: Int,
    logLevel: LogLevel,
    logLevelColor: Boolean,
    logSuppressionList: Seq[String],
    actorTimeout: FiniteDuration,
    subscribeBatchSize: Int,
    subscribeBatchInterval: FiniteDuration,
    subscribeBufferSize: Int
):
  def validated(path: String): ValidatedNec[String, StaticCenterConfig] =
    import cats.syntax.apply.*
    (
      clock.validated(s"$path.clock"),
      condNec(
        runIdPrefix.nonEmpty,
        runIdPrefix,
        s"$path.runIdPrefix must not be empty"
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
        actorTimeout > 0.second,
        actorTimeout,
        s"$path.actorTimeout must be > 0"
      ),
      condNec(
        subscribeBatchSize > 0,
        subscribeBatchSize,
        s"$path.subscribeBatchSize must be > 0"
      ),
      condNec(
        subscribeBatchInterval > 0.second,
        subscribeBatchInterval,
        s"$path.subscribeBatchInterval must be > 0"
      ),
      condNec(
        subscribeBufferSize > 0,
        subscribeBufferSize,
        s"$path.subscribeBufferSize must be > 0"
      )
    ).mapN(StaticCenterConfig.apply)
