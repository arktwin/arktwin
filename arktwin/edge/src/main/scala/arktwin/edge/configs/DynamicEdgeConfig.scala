// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import cats.data.ValidatedNec

case class DynamicEdgeConfig(
    coordinate: CoordinateConfig,
    culling: CullingConfig
):
  def validated(path: String): ValidatedNec[String, DynamicEdgeConfig] =
    import cats.syntax.apply.*
    (
      coordinate.validated(s"$path.coordinate"),
      culling.validated(s"$path.culling")
    ).mapN(DynamicEdgeConfig.apply)
