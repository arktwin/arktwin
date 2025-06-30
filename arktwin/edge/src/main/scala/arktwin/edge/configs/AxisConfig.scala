// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.common.data.Vector3Enu
import arktwin.common.util.{EnumCaseInsensitiveConfigReader, EnumCaseInsensitiveJsonValueCodec}
import cats.data.Validated.condNec
import cats.data.ValidatedNec
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import pureconfig.ConfigReader
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

// cf. https://en.wikipedia.org/wiki/Local_tangent_plane_coordinates
case class AxisConfig(
    @description("x-axis direction")
    xDirection: AxisConfig.Direction,
    @description("y-axis direction")
    yDirection: AxisConfig.Direction,
    @description("z-axis direction")
    zDirection: AxisConfig.Direction
):
  def validated(path: String): ValidatedNec[String, AxisConfig] =
    import AxisConfig.Direction.*
    val sourceXyz = Seq(xDirection, yDirection, zDirection)
    condNec(
      (sourceXyz.contains(East) || sourceXyz.contains(West)) &&
        (sourceXyz.contains(North) || sourceXyz.contains(South)) &&
        (sourceXyz.contains(Up) || sourceXyz.contains(Down)),
      this,
      s"$path.{x, y, z} must contain (East or West) and (North or South) and (Up or Down)"
    )

  def toEnu(ax: Double, ay: Double, az: Double): (Double, Double, Double) =
    (
      xDirection.enu.x * ax + yDirection.enu.x * ay + zDirection.enu.x * az,
      xDirection.enu.y * ax + yDirection.enu.y * ay + zDirection.enu.y * az,
      xDirection.enu.z * ax + yDirection.enu.z * ay + zDirection.enu.z * az
    )

  def fromEnu(ax: Double, ay: Double, az: Double): (Double, Double, Double) =
    (
      xDirection.enu.x * ax + xDirection.enu.y * ay + xDirection.enu.z * az,
      yDirection.enu.x * ax + yDirection.enu.y * ay + yDirection.enu.z * az,
      zDirection.enu.x * ax + zDirection.enu.y * ay + zDirection.enu.z * az
    )

object AxisConfig:
  enum Direction(val enu: Vector3Enu):
    case East extends Direction(Vector3Enu(1, 0, 0))
    case West extends Direction(Vector3Enu(-1, 0, 0))
    case North extends Direction(Vector3Enu(0, 1, 0))
    case South extends Direction(Vector3Enu(0, -1, 0))
    case Up extends Direction(Vector3Enu(0, 0, 1))
    case Down extends Direction(Vector3Enu(0, 0, -1))
  object Direction:
    given Schema[Direction] = Schema.derivedEnumeration(encode = Some(_.toString))
    given ConfigReader[Direction] = EnumCaseInsensitiveConfigReader(values)
    given JsonValueCodec[Direction] = EnumCaseInsensitiveJsonValueCodec(values, East)
