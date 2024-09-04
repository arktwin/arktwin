// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.config

import arktwin.common.LoggerConfigurator.LogLevel

import scala.concurrent.duration.FiniteDuration

case class StaticCenterConfig(
    clock: ClockConfig,
    runIdPrefix: String,
    host: String,
    port: Int,
    logLevel: LogLevel,
    logLevelColor: Boolean,
    subscribeBatchSize: Int,
    subscribeBatchInterval: FiniteDuration,
    subscribeBufferSize: Int
)
