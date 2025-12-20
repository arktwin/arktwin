// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

trait TransformEnuTrait:
  val timestamp: VirtualTimestamp
  val parentAgent: Option[String]
  val globalScale: Vector3Enu
  val localRotation: QuaternionEnu
  val localTranslationMeter: Vector3Enu
  val localTranslationSpeedMps: Vector3Enu
  val extra: Map[String, String]

  def extrapolate(targetVirtualTimestamp: VirtualTimestamp): TransformEnu =
    TransformEnu(
      targetVirtualTimestamp,
      parentAgent,
      globalScale,
      localRotation,
      localTranslationMeter +
        (localTranslationSpeedMps * (targetVirtualTimestamp - timestamp).secondsDouble),
      localTranslationSpeedMps,
      extra
    )
