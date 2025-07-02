// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

sealed trait TimeTag
class MachineTag extends TimeTag
class VirtualTag extends TimeTag
