// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.edge.data.Vector3
import cats.data.Validated.valid
import cats.data.ValidatedNec
import pureconfig.generic.derivation.EnumConfigReader
import sttp.tapir.Schema.annotations.description

case class CoordinateConfig(
    @description("global origin in the local coordinate system")
    globalOrigin: Vector3,
    @description("axis configuration")
    axis: AxisConfig,
    @description("rotation configuration")
    rotation: RotationConfig,
    @description("length unit")
    lengthUnit: CoordinateConfig.LengthUnit,
    @description("time unit")
    timeUnit: CoordinateConfig.TimeUnit
):
  def validated(path: String): ValidatedNec[String, CoordinateConfig] =
    import cats.syntax.apply.*
    (
      valid(globalOrigin),
      axis.validated(s"$path.axis"),
      rotation.validated(s"$path.rotation"),
      valid(lengthUnit),
      valid(timeUnit)
    ).mapN(CoordinateConfig.apply)

object CoordinateConfig:
  enum LengthUnit(val scale: Double) derives EnumConfigReader:
    case Millimeter extends LengthUnit(1e-3)
    case Centimeter extends LengthUnit(1e-2)
    case Meter extends LengthUnit(1)
    case Kilometer extends LengthUnit(1e3)

  enum TimeUnit(val scale: Double) derives EnumConfigReader:
    case Second extends TimeUnit(1)
    case Minute extends TimeUnit(60)
    case Hour extends TimeUnit(3600)
