// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common.data

object Vector3EnuEx:
  implicit class Vector3EnuExtension(val a: Vector3Enu) extends AnyVal:
    def +(that: Vector3Enu): Vector3Enu = Vector3Enu(a.x + that.x, a.y + that.y, a.z + that.z)

    def -(that: Vector3Enu): Vector3Enu = Vector3Enu(a.x - that.x, a.y - that.y, a.z - that.z)

    def *(that: Double): Vector3Enu = Vector3Enu(a.x * that, a.y * that, a.z * that)

    def /(that: Double): Vector3Enu = Vector3Enu(a.x / that, a.y / that, a.z / that)

    def distance(that: Vector3Enu): Double =
      val dx = a.x - that.x
      val dy = a.y - that.y
      val dz = a.z - that.z
      math.sqrt(dx * dx + dy * dy + dz * dz)

    def toDoubles: Seq[Double] = Seq(a.x, a.y, a.z)
