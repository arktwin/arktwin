// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import cats.data.ValidatedNec

case class EdgeDynamicConfig(
    chart: ChartConfig,
    coordinate: CoordinateConfig
):
  def validated(path: String): ValidatedNec[String, EdgeDynamicConfig] =
    import cats.syntax.apply.*
    (
      chart.validated(s"$path.chart"),
      coordinate.validated(s"$path.coordinate")
    ).mapN(EdgeDynamicConfig.apply)
