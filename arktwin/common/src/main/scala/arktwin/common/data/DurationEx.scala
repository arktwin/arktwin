// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

object DurationEx:
  extension (a: Duration)
    inline def tagMachine: MachineDuration =
      TaggedDuration.from[MachineTag](a)

    inline def tagVirtual: VirtualDuration =
      TaggedDuration.from[VirtualTag](a)
