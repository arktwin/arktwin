// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import cats.data.ValidatedNec

// TODO how to reflect rotation to ENU?
case class CoordinateConfig(
    vector3: Vector3Config,
    rotation: RotationConfig
):
  def validated(path: String): ValidatedNec[String, CoordinateConfig] =
    import cats.syntax.apply.*
    (
      vector3.validated(s"$path.vector3"),
      rotation.validated(s"$path.rotation")
    ).mapN(CoordinateConfig.apply)
