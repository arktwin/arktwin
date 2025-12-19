// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

trait Vector3EnuTrait:
  val x: Double
  val y: Double
  val z: Double

  def +(that: Vector3Enu): Vector3Enu = Vector3Enu(x + that.x, y + that.y, z + that.z)

  def -(that: Vector3Enu): Vector3Enu = Vector3Enu(x - that.x, y - that.y, z - that.z)

  def *(that: Double): Vector3Enu = Vector3Enu(x * that, y * that, z * that)

  def /(that: Double): Vector3Enu = Vector3Enu(x / that, y / that, z / that)

  def distance(that: Vector3Enu): Double =
    val dx = x - that.x
    val dy = y - that.y
    val dz = z - that.z
    math.sqrt(dx * dx + dy * dy + dz * dz)
