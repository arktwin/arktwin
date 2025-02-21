// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.common.util.EnumConfigIdentityReader
import arktwin.edge.data.Vector3
import cats.data.Validated.valid
import cats.data.ValidatedNec
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

case class CoordinateConfig(
    @description("axis configuration")
    axis: AxisConfig,
    @description("center's origin in the edge's coordinate system")
    centerOrigin: Vector3,
    @description("rotation configuration")
    rotation: RotationConfig,
    @description("length unit")
    lengthUnit: CoordinateConfig.LengthUnit,
    @description("speed unit")
    speedUnit: CoordinateConfig.SpeedUnit
):
  def validated(path: String): ValidatedNec[String, CoordinateConfig] =
    import cats.syntax.apply.*
    (
      axis.validated(s"$path.axis"),
      valid(centerOrigin),
      rotation.validated(s"$path.rotation"),
      valid(lengthUnit),
      valid(speedUnit)
    ).mapN(CoordinateConfig.apply)

object CoordinateConfig:
  enum LengthUnit(val scale: Double) derives EnumConfigIdentityReader:
    case Millimeter extends LengthUnit(1e-3)
    case Centimeter extends LengthUnit(1e-2)
    case Meter extends LengthUnit(1)
    case Kilometer extends LengthUnit(1e3)
  object LengthUnit:
    given Schema[LengthUnit] = Schema.derivedEnumeration[LengthUnit](encode = Some(_.toString))

  enum SpeedUnit(val scale: Double) derives EnumConfigIdentityReader:
    case MillimeterPerSecond extends SpeedUnit(1e-3)
    case CentimeterPerSecond extends SpeedUnit(1e-2)
    case MeterPerSecond extends SpeedUnit(1)
    case KilometerPerSecond extends SpeedUnit(1e3)

    case MillimeterPerMinute extends SpeedUnit(1e-3 / 60.0)
    case CentimeterPerMinute extends SpeedUnit(1e-2 / 60.0)
    case MeterPerMinute extends SpeedUnit(1 / 60.0)
    case KilometerPerMinute extends SpeedUnit(1e3 / 60.0)

    case MillimeterPerHour extends SpeedUnit(1e-3 / 3600.0)
    case CentimeterPerHour extends SpeedUnit(1e-2 / 3600.0)
    case MeterPerHour extends SpeedUnit(1 / 3600.0)
    case KilometerPerHour extends SpeedUnit(1e3 / 3600.0)
  object SpeedUnit:
    given Schema[SpeedUnit] = Schema.derivedEnumeration[SpeedUnit](encode = Some(_.toString))
