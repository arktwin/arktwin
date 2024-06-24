/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.edge.data

import arktwin.common.data.QuaternionEnu
import arktwin.common.data.QuaternionEnuEx.*
import arktwin.edge.util.JsonDerivation
import pureconfig.ConfigReader
import pureconfig.generic.derivation.EnumConfigReader
import sttp.tapir.*

import scala.math.*

// TODO reflect ENU
sealed trait Rotation:
  def toQuaternionEnu(config: RotationConfig): QuaternionEnu

object Rotation:
  given Schema[Rotation] = JsonDerivation.fixCoproductSchemaWithoutDiscriminator(Schema.derived)

  def fromQuaternionEnu(a: QuaternionEnu, config: RotationConfig): Rotation = config match
    case QuaternionConfig =>
      Quaternion(a.x, a.y, a.z, a.w)

    case EulerAnglesConfig(angleUnit, order) =>
      import EulerAnglesConfig.*

      val (rx, ry, rz) = order match
        case Order.XYZ =>
          (
            atan2(2 * (a.w * a.x + a.y * a.z), 1 - 2 * (a.y * a.y + a.x * a.x)),
            asin(2 * (a.w * a.y - a.x * a.z)),
            atan2(2 * (a.w * a.z + a.y * a.x), 1 - 2 * (a.y * a.y + a.z * a.z))
          )
        case Order.XZY =>
          (
            atan2(2 * (a.w * a.x - a.z * a.y), 1 - 2 * (a.z * a.z + a.x * a.x)),
            atan2(2 * (a.w * a.y - a.z * a.x), 1 - 2 * (a.z * a.z + a.y * a.y)),
            asin(2 * (a.w * a.z + a.x * a.y))
          )
        case Order.YXZ =>
          (
            asin(2 * (a.w * a.x + a.y * a.z)),
            atan2(2 * (a.w * a.y - a.x * a.z), 1 - 2 * (a.x * a.x + a.y * a.y)),
            atan2(2 * (a.w * a.z - a.x * a.y), 1 - 2 * (a.x * a.x + a.z * a.z))
          )
        case Order.YZX =>
          (
            atan2(2 * (a.w * a.x + a.z * a.y), 1 - 2 * (a.z * a.z + a.x * a.x)),
            atan2(2 * (a.w * a.y + a.z * a.x), 1 - 2 * (a.z * a.z + a.y * a.y)),
            asin(2 * (a.w * a.z - a.x * a.y))
          )
        case Order.ZXY =>
          (
            asin(2 * (a.w * a.x - a.y * a.z)),
            atan2(2 * (a.w * a.y + a.x * a.z), 1 - 2 * (a.x * a.x + a.y * a.y)),
            atan2(2 * (a.w * a.z + a.x * a.y), 1 - 2 * (a.x * a.x + a.z * a.z))
          )
        case Order.ZYX =>
          (
            atan2(2 * (a.w * a.x - a.y * a.z), 1 - 2 * (a.y * a.y + a.x * a.x)),
            asin(2 * (a.w * a.y + a.x * a.z)),
            atan2(2 * (a.w * a.z - a.y * a.x), 1 - 2 * (a.y * a.y + a.z * a.z))
          )

      angleUnit match
        case AngleUnit.Degree => EulerAngles(toDegrees(rx), toDegrees(ry), toDegrees(rz))
        case AngleUnit.Radian => EulerAngles(rx, ry, rz)

case class Quaternion(
    x: Double,
    y: Double,
    z: Double,
    w: Double
) extends Rotation:
  override def toQuaternionEnu(config: RotationConfig): QuaternionEnu = config match
    case QuaternionConfig =>
      QuaternionEnu(x, y, z, w)

    case _ =>
      throw IllegalArgumentException()

case class EulerAngles(
    x: Double,
    y: Double,
    z: Double
) extends Rotation:
  def toQuaternionEnu(config: RotationConfig): QuaternionEnu = config match
    case EulerAnglesConfig(angleUnit, order) =>
      import EulerAnglesConfig.*

      val (rx, ry, rz) = angleUnit match
        case AngleUnit.Degree =>
          (toRadians(x), toRadians(y), toRadians(z))
        case AngleUnit.Radian =>
          (x, y, z)

      val qx = QuaternionEnu(sin(rx / 2), 0, 0, cos(rx / 2))
      val qy = QuaternionEnu(0, sin(ry / 2), 0, cos(ry / 2))
      val qz = QuaternionEnu(0, 0, sin(rz / 2), cos(rz / 2))

      order match
        case Order.XYZ => qz * qy * qx
        case Order.XZY => qy * qz * qx
        case Order.YXZ => qz * qx * qy
        case Order.YZX => qx * qz * qy
        case Order.ZXY => qy * qx * qz
        case Order.ZYX => qx * qy * qz

    case _ =>
      throw IllegalArgumentException()

// TODO RotationMatrix

sealed trait RotationConfig

object RotationConfig:
  given Schema[RotationConfig] = JsonDerivation.fixCoproductSchemaWithoutDiscriminator(Schema.derived)

case object QuaternionConfig extends RotationConfig

case class EulerAnglesConfig(
    angleUnit: EulerAnglesConfig.AngleUnit,
    order: EulerAnglesConfig.Order
) extends RotationConfig

object EulerAnglesConfig:
  enum AngleUnit derives EnumConfigReader:
    case Degree, Radian
  object AngleUnit:
    given Schema[AngleUnit] = Schema.derivedEnumeration[AngleUnit](encode = Some(_.toString))

  enum Order derives EnumConfigReader:
    case XYZ, XZY, YXZ, YZX, ZXY, ZYX
  object Order:
    given Schema[Order] = Schema.derivedEnumeration[Order](encode = Some(_.toString))
