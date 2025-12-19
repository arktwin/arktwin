// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.util.{MachineTag, VirtualTag}

trait TimestampTrait:
  val seconds: Long
  val nanos: Int

  inline def tagMachine: MachineTimestamp =
    TaggedTimestamp.from[MachineTag](this)

  inline def tagVirtual: VirtualTimestamp =
    TaggedTimestamp.from[VirtualTag](this)
