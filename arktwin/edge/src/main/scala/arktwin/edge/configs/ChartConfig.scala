// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec
import sttp.tapir.Schema.annotations.description

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class ChartConfig(
    @description("If true, edge culling is enabled.")
    culling: Boolean,
    @description("If first agents is greater than cullingMaxFirstAgents, edge culling is disabled.")
    cullingMaxFirstAgents: Int,
    expiration: Boolean,
    expirationCheckMachineInterval: FiniteDuration,
    expirationTimeout: FiniteDuration
):
  def validated(path: String): ValidatedNec[String, ChartConfig] =
    import cats.syntax.apply.*
    (
      valid(culling),
      condNec(
        cullingMaxFirstAgents >= 0,
        cullingMaxFirstAgents,
        s"$path.cullingMaxFirstAgents must be >= 0"
      ),
      valid(expiration),
      condNec(
        expirationCheckMachineInterval > 0.second,
        expirationCheckMachineInterval,
        s"$path.expirationCheckMachineInterval must be > 0"
      ),
      condNec(
        expirationTimeout > 0.second,
        expirationTimeout,
        s"$path.expirationTimeout must be > 0"
      )
    ).mapN(ChartConfig.apply)
