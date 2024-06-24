/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.edge.data

import arktwin.common.data.Vector3Enu
import arktwin.common.data.Vector3EnuEx.*
import pureconfig.ConfigReader
import pureconfig.generic.derivation.EnumConfigReader

case class Vector3(
    x: Double,
    y: Double,
    z: Double
):
  def +(that: Vector3): Vector3 = Vector3(x + that.x, y + that.y, z + that.z)

  def -(that: Vector3): Vector3 = Vector3(x - that.x, y - that.y, z - that.z)

  def *(that: Double): Vector3 = Vector3(x * that, y * that, z * that)

  def /(that: Double): Vector3 = Vector3(x / that, y / that, z / that)

  def toDoubles: Seq[Double] = Seq(x, y, z)

  def toEnu(config: Vector3Config): Vector3Enu =
    config.toDirections
      .map(_.toEnu)
      .zip(toDoubles)
      .map((a, b) => a * b)
      .reduce(_ + _)

object Vector3:
  private def fromDoubles(source: Seq[Double]): Vector3 =
    Vector3(source.head, source(1), source(2))

  def fromEnu(source: Vector3Enu, config: Vector3Config): Vector3 =
    config.toDirections
      .map(_.toEnu.toDoubles)
      .transpose
      .map(fromDoubles)
      .zip(source.toDoubles)
      .map((a, b) => a * b)
      .reduce(_ + _)

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
