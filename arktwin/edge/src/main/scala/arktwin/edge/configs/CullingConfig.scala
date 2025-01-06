// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec
import sttp.tapir.Schema.annotations.description

case class CullingConfig(
    @description("If true, edge culling is enabled.")
    edgeCulling: Boolean,
    @description("If first agents is greater than maxFirstAgents, edge culling is disabled.")
    maxFirstAgents: Int
):
  def validated(path: String): ValidatedNec[String, CullingConfig] =
    import cats.syntax.apply.*
    (
      valid(edgeCulling),
      condNec(
        maxFirstAgents >= 0,
        maxFirstAgents,
        s"$path.maxFirstAgents must be >= 0"
      )
    ).mapN(CullingConfig.apply)
