// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.common.data.Vector3Enu
import cats.data.Validated.condNec
import cats.data.ValidatedNec
import pureconfig.ConfigReader
import pureconfig.generic.derivation.EnumConfigReader

// cf. https://en.wikipedia.org/wiki/Local_tangent_plane_coordinates
// TODO (latitude, longitude, elevation) origin?
case class Vector3Config(
    lengthUnit: Vector3Config.LengthUnit,
    speedUnit: Vector3Config.TimeUnit,
    x: Vector3Config.Direction,
    y: Vector3Config.Direction,
    z: Vector3Config.Direction
):
  def validated(path: String): ValidatedNec[String, Vector3Config] =
    import Vector3Config.Direction.*
    val sourceXyz = Seq(x, y, z)
    condNec(
      (sourceXyz.contains(East) || sourceXyz.contains(West)) &&
        (sourceXyz.contains(North) || sourceXyz.contains(South)) &&
        (sourceXyz.contains(Up) || sourceXyz.contains(Down)),
      this,
      s"$path.{x, y, z} must contain (East or West) and (North or South) and (Up or Down)"
    )

  def toDirections: Seq[Vector3Config.Direction] = Seq(x, y, z)

object Vector3Config:
  // TODO scale
  enum LengthUnit derives EnumConfigReader:
    case Meter

  // TODO scale
  enum TimeUnit derives EnumConfigReader:
    case Second

  enum Direction(val toEnu: Vector3Enu) derives EnumConfigReader:
    case East extends Direction(Vector3Enu(1, 0, 0))
    case West extends Direction(Vector3Enu(-1, 0, 0))
    case North extends Direction(Vector3Enu(0, 1, 0))
    case South extends Direction(Vector3Enu(0, -1, 0))
    case Up extends Direction(Vector3Enu(0, 0, 1))
    case Down extends Direction(Vector3Enu(0, 0, -1))
