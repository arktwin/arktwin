// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.common.util.{
  EnumCaseInsensitiveConfigReader,
  EnumCaseInsensitiveJsonValueCodec,
  JsonDerivation
}
import cats.data.Validated.valid
import cats.data.ValidatedNec
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import pureconfig.ConfigReader
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description

sealed trait RotationConfig:
  def validated(path: String): ValidatedNec[String, RotationConfig]

object RotationConfig:
  given Schema[RotationConfig] =
    JsonDerivation.fixCoproductSchemaWithoutDiscriminator(Schema.derived)

case object QuaternionConfig extends RotationConfig:
  override def validated(path: String): ValidatedNec[String, QuaternionConfig.type] =
    valid(this)

case class EulerAnglesConfig(
    @description("angle unit")
    angleUnit: EulerAnglesConfig.AngleUnit,
    @description(
      "rotation mode: extrinsic rotation (edge's world space rotation) or intrinsic rotation (agent's local space rotation)"
    )
    rotationMode: EulerAnglesConfig.RotationMode,
    @description(
      "rotation order: For example, XYZ means rotate around X axis first, then Y axis, and finally Z axis"
    )
    rotationOrder: EulerAnglesConfig.RotationOrder
) extends RotationConfig:
  override def validated(path: String): ValidatedNec[String, EulerAnglesConfig] =
    valid(this)

object EulerAnglesConfig:
  enum AngleUnit:
    case Degree, Radian
  object AngleUnit:
    given Schema[AngleUnit] = Schema.derivedEnumeration(encode = Some(_.toString))
    given ConfigReader[AngleUnit] = EnumCaseInsensitiveConfigReader(values)
    given JsonValueCodec[AngleUnit] = EnumCaseInsensitiveJsonValueCodec(values, Degree)

  enum RotationMode:
    case Extrinsic, Intrinsic
  object RotationMode:
    given Schema[RotationMode] = Schema.derivedEnumeration(encode = Some(_.toString))
    given ConfigReader[RotationMode] = EnumCaseInsensitiveConfigReader(values)
    given JsonValueCodec[RotationMode] = EnumCaseInsensitiveJsonValueCodec(values, Extrinsic)

  enum RotationOrder:
    case XYZ, XZY, YXZ, YZX, ZXY, ZYX
  object RotationOrder:
    given Schema[RotationOrder] = Schema.derivedEnumeration(encode = Some(_.toString))
    given ConfigReader[RotationOrder] = EnumCaseInsensitiveConfigReader(values)
    given JsonValueCodec[RotationOrder] = EnumCaseInsensitiveJsonValueCodec(values, XYZ)
