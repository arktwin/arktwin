// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import org.scalatest.funspec.AnyFunSpec

class TransformEnuSpec extends AnyFunSpec:
  val defaultParentAgent = Some("parentABC")
  val defaultGlobalScale = Vector3Enu(1, 2, 3)
  val defaultLocalRotation = QuaternionEnu(0.1, 0.2, 0.3, 0.927362)
  val defaultExtra = Map("key1" -> "value1", "key2" -> "value2")
  def createTransformEnu(
      seconds: Long,
      x: Double,
      y: Double,
      z: Double,
      vx: Double,
      vy: Double,
      vz: Double
  ): TransformEnu =
    TransformEnu(
      timestamp = VirtualTimestamp(seconds, 0),
      parentAgent = defaultParentAgent,
      globalScale = defaultGlobalScale,
      localRotation = defaultLocalRotation,
      localTranslationMeter = Vector3Enu(x, y, z),
      localTranslationSpeedMps = Vector3Enu(vx, vy, vz),
      extra = defaultExtra
    )

  describe("TransformEnu"):
    describe("extrapolate"):
      it("updates position with zero time difference"):
        val transform = createTransformEnu(100, 10.0, 20.0, 30.0, 1.0, 2.0, 3.0)
        val targetTime = VirtualTimestamp(100, 0)
        val result = transform.extrapolate(targetTime)

        assert(result.timestamp == targetTime)
        assert(result.localTranslationMeter == Vector3Enu(10.0, 20.0, 30.0))
        assert(result.localTranslationSpeedMps == Vector3Enu(1.0, 2.0, 3.0))

      it("updates position forward in time"):
        val transform = createTransformEnu(100, 10.0, 20.0, 30.0, 1.0, 2.0, 3.0)
        val targetTime = VirtualTimestamp(102, 0) // 2 seconds later
        val result = transform.extrapolate(targetTime)

        assert(result.timestamp == targetTime)
        assert(result.localTranslationMeter == Vector3Enu(12.0, 24.0, 36.0))
        assert(result.localTranslationSpeedMps == Vector3Enu(1.0, 2.0, 3.0))

      it("updates position backward in time"):
        val transform = createTransformEnu(100, 10.0, 20.0, 30.0, 1.0, 2.0, 3.0)
        val targetTime = VirtualTimestamp(98, 0)
        val result = transform.extrapolate(targetTime)

        assert(result.timestamp == targetTime)
        assert(result.localTranslationMeter == Vector3Enu(8.0, 16.0, 24.0))
        assert(result.localTranslationSpeedMps == Vector3Enu(1.0, 2.0, 3.0))

      it("updates position with fractional seconds"):
        val transform = createTransformEnu(100, 0.0, 0.0, 0.0, 10.0, 20.0, 30.0)
        val targetTime = VirtualTimestamp(100, 500000000) // 0.5 seconds later
        val result = transform.extrapolate(targetTime)

        assert(result.timestamp == targetTime)
        assert(result.localTranslationMeter == Vector3Enu(5.0, 10.0, 15.0))

      it("maintains position with zero speed"):
        val transform = createTransformEnu(100, 10.0, 20.0, 30.0, 0.0, 0.0, 0.0)
        val targetTime = VirtualTimestamp(105, 0) // 5 seconds later
        val result = transform.extrapolate(targetTime)

        assert(result.timestamp == targetTime)
        assert(result.localTranslationMeter == Vector3Enu(10.0, 20.0, 30.0))

      it("updates position with negative speed"):
        val transform = createTransformEnu(100, 50.0, 60.0, 70.0, -5.0, -10.0, -15.0)
        val targetTime = VirtualTimestamp(102, 0) // 2 seconds later
        val result = transform.extrapolate(targetTime)

        assert(result.localTranslationMeter == Vector3Enu(40.0, 40.0, 40.0))

      it("preserves other fields during extrapolation"):
        val transform = createTransformEnu(100, 1.0, 2.0, 3.0, 0.5, 0.5, 0.5)
        val targetTime = VirtualTimestamp(101, 0)
        val result = transform.extrapolate(targetTime)

        assert(result.parentAgent == defaultParentAgent)
        assert(result.globalScale == defaultGlobalScale)
        assert(result.localRotation == defaultLocalRotation)
        assert(result.extra == defaultExtra)
