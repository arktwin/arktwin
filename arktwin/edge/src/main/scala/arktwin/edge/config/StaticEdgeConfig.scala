// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.config

import arktwin.common.LoggerConfigurator.LogLevel

import scala.concurrent.duration.FiniteDuration

case class StaticEdgeConfig(
    edgeIdPrefix: String,
    host: String,
    port: Int,
    logLevel: LogLevel,
    logLevelColor: Boolean,
    actorTimeout: FiniteDuration,
    endpointTimeout: FiniteDuration,
    clockInitialStashSize: Int,
    publishBatchSize: Int,
    publishBufferSize: Int
)
