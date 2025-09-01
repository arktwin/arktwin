// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import cats.data.ValidatedNec

// TODO changeable via Admin API
case class CenterDynamicConfig(
    atlas: AtlasConfig
):
  def validated(path: String): ValidatedNec[String, CenterDynamicConfig] =
    atlas.validated(s"$path.atlas").map(CenterDynamicConfig.apply)
