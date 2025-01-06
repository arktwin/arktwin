// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.data.Vector3Enu
import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class AtlasConfig(
    culling: AtlasConfig.Culling,
    routeTableUpdateInterval: FiniteDuration
):
  def validated(path: String): ValidatedNec[String, AtlasConfig] =
    import cats.syntax.apply.*
    (
      culling.validated(s"$path.culling"),
      condNec(
        routeTableUpdateInterval > 0.second,
        routeTableUpdateInterval,
        s"$path.routeTableUpdateInterval must be > 0"
      )
    ).mapN(AtlasConfig.apply)

object AtlasConfig:
  sealed trait Culling:
    def validated(path: String): ValidatedNec[String, Culling]

  case class Broadcast() extends Culling:
    override def validated(path: String): ValidatedNec[String, Broadcast] =
      valid(this)

  case class GridCulling(gridCellSize: Vector3Enu) extends Culling:
    override def validated(path: String): ValidatedNec[String, GridCulling] =
      condNec(
        gridCellSize.x > 0 && gridCellSize.y > 0 && gridCellSize.z > 0,
        gridCellSize,
        s"$path.gridCellSize.{x, y, z} must be > 0"
      ).map(GridCulling.apply)
