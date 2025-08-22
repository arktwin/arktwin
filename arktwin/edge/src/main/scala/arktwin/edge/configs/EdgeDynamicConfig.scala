// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import cats.data.ValidatedNec

case class EdgeDynamicConfig(
    coordinate: CoordinateConfig,
    culling: CullingConfig
):
  def validated(path: String): ValidatedNec[String, EdgeDynamicConfig] =
    import cats.syntax.apply.*
    (
      coordinate.validated(s"$path.coordinate"),
      culling.validated(s"$path.culling")
    ).mapN(EdgeDynamicConfig.apply)
