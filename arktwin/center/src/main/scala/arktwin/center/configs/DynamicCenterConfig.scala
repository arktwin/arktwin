// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import cats.data.ValidatedNec

// TODO changeable via Admin API
case class DynamicCenterConfig(
    atlas: AtlasConfig
):
  def validated(path: String): ValidatedNec[String, DynamicCenterConfig] =
    atlas.validated(s"$path.atlas").map(DynamicCenterConfig.apply)
