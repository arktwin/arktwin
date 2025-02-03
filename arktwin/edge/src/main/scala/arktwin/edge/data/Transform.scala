// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.*
import arktwin.common.data.TimestampExtensions.*
import arktwin.common.data.Vector3EnuExtensions.*
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
  def normalize(
      timestamp: VirtualTimestamp,
      previousEnu: Option[TransformEnu],
      config: CoordinateConfig
  ): TransformEnu =
    val localTranslationEnuMeter =
      (localTranslation - config.globalOrigin).normalize(config.axis) * config.lengthUnit.scale
    val localTranslationSpeedEnuMps = localTranslationSpeed
      .map(_.normalize(config.axis) * config.speedUnit.scale)
      .getOrElse(
        previousEnu match
          case Some(pEnu) if timestamp > pEnu.timestamp.tagVirtual =>
            (localTranslationEnuMeter - pEnu.localTranslationMeter) / (timestamp - pEnu.timestamp.tagVirtual).secondsDouble
          case _ =>
            Vector3Enu(0, 0, 0)
      )

    TransformEnu(
      timestamp.untag,
      parentAgentId,
      globalScale.normalize(config.axis),
      localRotation.normalize(config.rotation, config.axis),
      localTranslationEnuMeter,
      localTranslationSpeedEnuMps,
      extra.getOrElse(Map())
    )

object Transform:
  def apply(a: TransformEnu, config: CoordinateConfig): Transform =
    Transform(
      a.parentAgent,
      Vector3(a.globalScale, config.axis),
      Rotation(a.localRotation, config.rotation, config.axis),
      (Vector3(
        a.localTranslationMeter,
        config.axis
      ) / config.lengthUnit.scale) + config.globalOrigin,
      Some(
        Vector3(
          a.localTranslationSpeedMps,
          config.axis
        ) / config.speedUnit.scale
      ),
      if a.extra.nonEmpty then Some(a.extra) else None
    )
