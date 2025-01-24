// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.common.EnumConfigIdentityReader
import arktwin.common.data.QuaternionEnuEx.*
import arktwin.edge.util.JsonDerivation
import cats.data.Validated.valid
import cats.data.ValidatedNec
import pureconfig.ConfigReader
import sttp.tapir.*
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
    @description("rotation order: For example, XYZ means rotate around X axis first, then Y axis, and finally Z axis")
    rotationOrder: EulerAnglesConfig.RotationOrder
) extends RotationConfig:
  override def validated(path: String): ValidatedNec[String, EulerAnglesConfig] =
    valid(this)

object EulerAnglesConfig:
  enum AngleUnit derives EnumConfigIdentityReader:
    case Degree, Radian
  object AngleUnit:
    given Schema[AngleUnit] = Schema.derivedEnumeration[AngleUnit](encode = Some(_.toString))

  enum RotationOrder derives EnumConfigIdentityReader:
    case XYZ, XZY, YXZ, YZX, ZXY, ZYX
  object RotationOrder:
    given Schema[RotationOrder] =
      Schema.derivedEnumeration[RotationOrder](encode = Some(_.toString))
