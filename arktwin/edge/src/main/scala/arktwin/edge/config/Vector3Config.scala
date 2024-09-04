// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.config

import arktwin.common.data.Vector3Enu
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
  import Vector3Config.Direction.*

  def toDirections: Seq[Vector3Config.Direction] = Seq(x, y, z)

  // TODO validate
  def assertXyz(): Unit =
    val sourceXyz = Seq(x, y, z)

    assert(sourceXyz.contains(Up) || sourceXyz.contains(Down))
    assert(sourceXyz.contains(North) || sourceXyz.contains(South))
    assert(sourceXyz.contains(East) || sourceXyz.contains(West))

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
