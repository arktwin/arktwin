// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.QuaternionEnu
import arktwin.common.data.QuaternionEnuExtensions.*
import arktwin.edge.configs.EulerAnglesConfig.{AngleUnit, RotationOrder}
import arktwin.edge.configs.{AxisConfig, EulerAnglesConfig, QuaternionConfig, RotationConfig}
import arktwin.edge.util.JsonDerivation
import sttp.tapir.*

import scala.math.*

sealed trait Rotation:
  def normalize(rotationConfig: RotationConfig, axisConfig: AxisConfig): QuaternionEnu

case class Quaternion(
    x: Double,
    y: Double,
    z: Double,
    w: Double
) extends Rotation:
  override def normalize(rotationConfig: RotationConfig, axisConfig: AxisConfig): QuaternionEnu =
    rotationConfig match
      case QuaternionConfig =>
        val a = axisConfig.toEnu(x, y, z)
        QuaternionEnu(a._1, a._2, a._3, w)

      case _ =>
        throw IllegalArgumentException()

case class EulerAngles(
    x: Double,
    y: Double,
    z: Double
) extends Rotation:
  override def normalize(rotationConfig: RotationConfig, axisConfig: AxisConfig): QuaternionEnu =
    rotationConfig match
      case EulerAnglesConfig(angleUnit, order) =>
        val (rx, ry, rz) = angleUnit match
          case AngleUnit.Degree =>
            (toRadians(x), toRadians(y), toRadians(z))
          case AngleUnit.Radian =>
            (x, y, z)

        val qx = QuaternionEnu(sin(rx / 2), 0, 0, cos(rx / 2))
        val qy = QuaternionEnu(0, sin(ry / 2), 0, cos(ry / 2))
        val qz = QuaternionEnu(0, 0, sin(rz / 2), cos(rz / 2))

        val q = order match
          case RotationOrder.XYZ => qz * qy * qx
          case RotationOrder.XZY => qy * qz * qx
          case RotationOrder.YXZ => qz * qx * qy
          case RotationOrder.YZX => qx * qz * qy
          case RotationOrder.ZXY => qy * qx * qz
          case RotationOrder.ZYX => qx * qy * qz

        val a = axisConfig.toEnu(q.x, q.y, q.z)
        QuaternionEnu(a._1, a._2, a._3, q.w)

      case _ =>
        throw IllegalArgumentException()

object Rotation:
  given Schema[Rotation] = JsonDerivation.fixCoproductSchemaWithoutDiscriminator(Schema.derived)

  def apply(a: QuaternionEnu, rotationConfig: RotationConfig, axisConfig: AxisConfig): Rotation =
    rotationConfig match
      case QuaternionConfig =>
        val b = axisConfig.fromEnu(a.x, a.y, a.z)
        Quaternion(b._1, b._2, b._3, a.w)

      case EulerAnglesConfig(angleUnit, order) =>
        val (bx, by, bz) = axisConfig.fromEnu(a.x, a.y, a.z)
        val bw = a.w

        val (rx, ry, rz) = order match
          case RotationOrder.XYZ =>
            (
              atan2(2 * (bw * bx + by * bz), 1 - 2 * (by * by + bx * bx)),
              asin(2 * (bw * by - bx * bz)),
              atan2(2 * (bw * bz + by * bx), 1 - 2 * (by * by + bz * bz))
            )
          case RotationOrder.XZY =>
            (
              atan2(2 * (bw * bx - bz * by), 1 - 2 * (bz * bz + bx * bx)),
              atan2(2 * (bw * by - bz * bx), 1 - 2 * (bz * bz + by * by)),
              asin(2 * (bw * bz + bx * by))
            )
          case RotationOrder.YXZ =>
            (
              asin(2 * (bw * bx + by * bz)),
              atan2(2 * (bw * by - bx * bz), 1 - 2 * (bx * bx + by * by)),
              atan2(2 * (bw * bz - bx * by), 1 - 2 * (bx * bx + bz * bz))
            )
          case RotationOrder.YZX =>
            (
              atan2(2 * (bw * bx + bz * by), 1 - 2 * (bz * bz + bx * bx)),
              atan2(2 * (bw * by + bz * bx), 1 - 2 * (bz * bz + by * by)),
              asin(2 * (bw * bz - bx * by))
            )
          case RotationOrder.ZXY =>
            (
              asin(2 * (bw * bx - by * bz)),
              atan2(2 * (bw * by + bx * bz), 1 - 2 * (bx * bx + by * by)),
              atan2(2 * (bw * bz + bx * by), 1 - 2 * (bx * bx + bz * bz))
            )
          case RotationOrder.ZYX =>
            (
              atan2(2 * (bw * bx - by * bz), 1 - 2 * (by * by + bx * bx)),
              asin(2 * (bw * by + bx * bz)),
              atan2(2 * (bw * bz - by * bx), 1 - 2 * (by * by + bz * bz))
            )

        angleUnit match
          case AngleUnit.Degree => EulerAngles(toDegrees(rx), toDegrees(ry), toDegrees(rz))
          case AngleUnit.Radian => EulerAngles(rx, ry, rz)
