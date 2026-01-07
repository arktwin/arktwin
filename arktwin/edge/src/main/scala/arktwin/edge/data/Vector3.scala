// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.Vector3Enu
import arktwin.edge.configs.AxisConfig

case class Vector3(
    x: Double,
    y: Double,
    z: Double
):
  def +(that: Vector3): Vector3 = Vector3(x + that.x, y + that.y, z + that.z)

  def -(that: Vector3): Vector3 = Vector3(x - that.x, y - that.y, z - that.z)

  def *(that: Double): Vector3 = Vector3(x * that, y * that, z * that)

  def /(that: Double): Vector3 = Vector3(x / that, y / that, z / that)

  def normalize(config: AxisConfig): Vector3Enu =
    Vector3Enu.apply.tupled(config.toEnu(x, y, z))

object Vector3:
  def apply(source: Vector3Enu, config: AxisConfig): Vector3 =
    Vector3.apply.tupled(config.fromEnu(source.x, source.y, source.z))
