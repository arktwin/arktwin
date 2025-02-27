// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

object QuaternionEnuExtensions:
  extension (a: QuaternionEnu)
    def *(b: QuaternionEnu): QuaternionEnu =
      QuaternionEnu(
        +a.w * b.x - a.z * b.y + a.y * b.z + a.x * b.w,
        +a.z * b.x + a.w * b.y - a.x * b.z + a.y * b.w,
        -a.y * b.x + a.x * b.y + a.w * b.z + a.z * b.w,
        -a.x * b.x - a.y * b.y - a.z * b.z + a.w * b.w
      )
