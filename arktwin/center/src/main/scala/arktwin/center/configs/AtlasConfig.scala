// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.data.Vector3Enu
import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class AtlasConfig(
    culling: AtlasConfig.Culling,
    routeTableUpdateMachineInterval: FiniteDuration
):
  def validated(path: String): ValidatedNec[String, AtlasConfig] =
    import cats.syntax.apply.*
    (
      culling.validated(s"$path.culling"),
      condNec(
        routeTableUpdateMachineInterval > 0.second,
        routeTableUpdateMachineInterval,
        s"$path.routeTableUpdateMachineInterval must be > 0"
      )
    ).mapN(AtlasConfig.apply)

object AtlasConfig:
  sealed trait Culling:
    def validated(path: String): ValidatedNec[String, Culling]

  case class Broadcast() extends Culling:
    override def validated(path: String): ValidatedNec[String, Broadcast] =
      valid(this)

  case class GridCulling(
      gridCellSizeCandidates: Seq[Vector3Enu],
      cellAgentNumUpperLimit: Int
  ) extends Culling:
    override def validated(path: String): ValidatedNec[String, GridCulling] =
      import cats.syntax.apply.*
      (
        (
          condNec(
            gridCellSizeCandidates.nonEmpty,
            gridCellSizeCandidates,
            s"$path.gridCellSizeCandidates must not be empty"
          ),
          condNec(
            gridCellSizeCandidates.forall(a => a.x > 0 && a.y > 0 && a.z > 0),
            gridCellSizeCandidates,
            s"$path.gridCellSizeCandidates elements.{x,y,z} must be > 0"
          ),
          condNec(
            gridCellSizeCandidates.distinct.size == gridCellSizeCandidates.size,
            gridCellSizeCandidates,
            s"$path.gridCellSizeCandidates elements must be unique"
          )
        ).mapN((_, _, _) => gridCellSizeCandidates),
        condNec(
          cellAgentNumUpperLimit >= 1,
          cellAgentNumUpperLimit,
          s"$path.cellAgentNumUpperLimit must be >= 1"
        )
      ).mapN(GridCulling.apply)
