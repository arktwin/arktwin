// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.util.LoggerConfigurator.LogLevel
import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class CenterStaticConfig(
    actorMachineTimeout: FiniteDuration,
    clock: ClockConfig,
    host: String,
    logLevel: LogLevel,
    logLevelColor: Boolean,
    logSuppressionList: Seq[String],
    port: Int,
    portAutoIncrement: Boolean,
    portAutoIncrementMax: Int,
    runIdPrefix: String,
    subscribeBatchMachineInterval: FiniteDuration,
    subscribeBatchSize: Int,
    subscribeBufferSize: Int
):
  def validated(path: String): ValidatedNec[String, CenterStaticConfig] =
    import cats.syntax.apply.*
    (
      condNec(
        actorMachineTimeout > 0.second,
        actorMachineTimeout,
        s"$path.actorMachineTimeout must be > 0"
      ),
      clock.validated(s"$path.clock"),
      condNec(
        host.nonEmpty,
        host,
        s"$path.host must not be empty"
      ),
      valid(logLevel),
      valid(logLevelColor),
      valid(logSuppressionList),
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
      condNec(
        runIdPrefix.nonEmpty,
        runIdPrefix,
        s"$path.runIdPrefix must not be empty"
      ),
      condNec(
        subscribeBatchMachineInterval > 0.second,
        subscribeBatchMachineInterval,
        s"$path.subscribeBatchMachineInterval must be > 0"
      ),
      condNec(
        subscribeBatchSize > 0,
        subscribeBatchSize,
        s"$path.subscribeBatchSize must be > 0"
      ),
      condNec(
        subscribeBufferSize > 0,
        subscribeBufferSize,
        s"$path.subscribeBufferSize must be > 0"
      )
    ).mapN(CenterStaticConfig.apply)
