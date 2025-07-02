// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data
import arktwin.common.util.{MachineTag, VirtualTag}

object TimestampExtensions:
  extension (a: Timestamp)
    inline def tagMachine: MachineTimestamp =
      TaggedTimestamp.from[MachineTag](a)

    inline def tagVirtual: VirtualTimestamp =
      TaggedTimestamp.from[VirtualTag](a)
