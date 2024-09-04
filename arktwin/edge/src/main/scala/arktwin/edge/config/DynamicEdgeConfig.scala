// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.config

import arktwin.edge.data.CoordinateConfig

case class DynamicEdgeConfig(
    coordinate: CoordinateConfig,
    culling: CullingConfig
)
