// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

// TODO how to reflect rotation to ENU?
case class CoordinateConfig(
    vector3: Vector3Config,
    rotation: RotationConfig
)
