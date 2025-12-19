// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

trait QuaternionEnuTrait:
  val x: Double
  val y: Double
  val z: Double
  val w: Double

  def *(b: QuaternionEnu): QuaternionEnu =
    QuaternionEnu(
      +w * b.x - z * b.y + y * b.z + x * b.w,
      +z * b.x + w * b.y - x * b.z + y * b.w,
      -y * b.x + x * b.y + w * b.z + z * b.w,
      -x * b.x - y * b.y - z * b.z + w * b.w
    )
