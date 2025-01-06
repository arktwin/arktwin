// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.*
import arktwin.common.data.TimestampEx.*
import arktwin.common.data.Vector3EnuEx.*
import arktwin.edge.configs.CoordinateConfig
import sttp.tapir.Schema.annotations.description

@description(
  "Transformation of position vectors is applied in the order: Scale > Rotate > Translate"
)
case class Transform(
    @description("It is a reserved field. Currently, it is not used in ArkTwin.")
    parentAgentId: Option[String],
    globalScale: Vector3,
    localRotation: Rotation,
    localTranslation: Vector3,
    @description(
      "If it is omitted, the edge calculates speed as a difference from a previous localTranslation."
    )
    localTranslationSpeed: Option[Vector3],
    @description("It should be updated each time.")
    extra: Option[Map[String, String]]
):
  def toEnu(
      timestamp: VirtualTimestamp,
      previousEnu: Option[TransformEnu],
      config: CoordinateConfig
  ): TransformEnu =
    val localTranslationEnu = localTranslation.toEnu(config.vector3)
    val localTranslationSpeedEnu = localTranslationSpeed
      .map(_.toEnu(config.vector3))
      .getOrElse(
        previousEnu match
          case Some(pEnu) if timestamp > pEnu.timestamp.tagVirtual =>
            (localTranslationEnu - pEnu.localTranslationMeter) / (timestamp - pEnu.timestamp.tagVirtual).secondsDouble
          case _ =>
            Vector3Enu(0, 0, 0)
      )

    TransformEnu(
      timestamp.untag,
      parentAgentId,
      globalScale.toEnu(config.vector3),
      localRotation.toQuaternionEnu(config.rotation),
      localTranslationEnu,
      localTranslationSpeedEnu,
      extra.getOrElse(Map())
    )

object Transform:
  def fromEnu(a: TransformEnu, config: CoordinateConfig): Transform =
    Transform(
      a.parentAgent,
      Vector3.fromEnu(a.globalScale, config.vector3),
      Rotation.fromQuaternionEnu(a.localRotation, config.rotation),
      Vector3.fromEnu(a.localTranslationMeter, config.vector3),
      Some(Vector3.fromEnu(a.localTranslationSpeedMps, config.vector3)),
      if a.extra.nonEmpty then Some(a.extra) else None
    )
