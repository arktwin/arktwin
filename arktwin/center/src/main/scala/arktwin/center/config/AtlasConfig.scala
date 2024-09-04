// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.config

import arktwin.common.data.Vector3Enu

import scala.concurrent.duration.FiniteDuration

case class AtlasConfig(
    culling: AtlasConfig.Culling,
    routeTableUpdateInterval: FiniteDuration
)

object AtlasConfig:
  sealed trait Culling
  case class Broadcast() extends Culling
  case class GridCulling(gridCellSize: Vector3Enu) extends Culling
