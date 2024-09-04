// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.Vector3Enu
import arktwin.common.data.Vector3EnuEx.*
import arktwin.edge.configs.Vector3Config

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
