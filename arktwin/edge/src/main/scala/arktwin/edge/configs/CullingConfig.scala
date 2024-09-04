// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import sttp.tapir.Schema.annotations.description

case class CullingConfig(
    @description("If true, edge culling is enabled.")
    edgeCulling: Boolean,
    @description("If first agents is greater than maxFirstAgents, edge culling is disabled.")
    maxFirstAgents: Int
)
