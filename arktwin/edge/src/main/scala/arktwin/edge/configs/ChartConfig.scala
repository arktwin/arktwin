// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec
import sttp.tapir.Schema.annotations.description

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class ChartConfig(
    culling: ChartConfig.Culling,
    expiration: ChartConfig.Expiration
):
  def validated(path: String): ValidatedNec[String, ChartConfig] =
    import cats.syntax.apply.*
    (
      culling.validated(s"$path.culling"),
      expiration.validated(s"$path.expiration")
    ).mapN(ChartConfig.apply)

object ChartConfig:
  case class Culling(
      @description("If true, edge culling is enabled.")
      enabled: Boolean,
      @description(
        "If first agents is greater than cullingMaxFirstAgents, edge culling is disabled."
      )
      maxFirstAgents: Int
  ):
    def validated(path: String): ValidatedNec[String, Culling] =
      import cats.syntax.apply.*
      (
        valid(enabled),
        condNec(
          maxFirstAgents >= 0,
          maxFirstAgents,
          s"$path.maxFirstAgents must be >= 0"
        )
      ).mapN(Culling.apply)

  case class Expiration(
      @description("If true, expired neighbors are automatically deleted from the chart.")
      enabled: Boolean,
      @description("Machine time interval to check for expired neighbors in the chart.")
      checkMachineInterval: FiniteDuration,
      @description(
        "Virtual time duration after which a neighbor is considered expired if not updated."
      )
      timeout: FiniteDuration
  ):
    def validated(path: String): ValidatedNec[String, Expiration] =
      import cats.syntax.apply.*
      (
        valid(enabled),
        condNec(
          checkMachineInterval > 0.seconds,
          checkMachineInterval,
          s"$path.checkMachineInterval must be > 0"
        ),
        condNec(
          timeout > 0.seconds,
          timeout,
          s"$path.timeout must be > 0"
        )
      ).mapN(Expiration.apply)
