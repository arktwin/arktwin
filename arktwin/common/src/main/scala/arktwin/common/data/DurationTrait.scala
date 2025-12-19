// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.util.{MachineTag, VirtualTag}

trait DurationTrait:
  val seconds: Long
  val nanos: Int

  inline def tagMachine: MachineDuration =
    TaggedDuration.from[MachineTag](this)

  inline def tagVirtual: VirtualDuration =
    TaggedDuration.from[VirtualTag](this)
