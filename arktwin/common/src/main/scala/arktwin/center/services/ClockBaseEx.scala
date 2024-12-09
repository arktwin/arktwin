// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.common.data.*
import arktwin.common.data.DurationEx.*
import arktwin.common.data.TimestampEx.*

object ClockBaseEx:
  extension (a: ClockBase)
    def fromMachine(machineTimestamp: MachineTimestamp): VirtualTimestamp =
      a.baseTimestamp.tagVirtual +
        ((machineTimestamp - a.baseMachineTimestamp.tagMachine) * a.clockSpeed).untag.tagVirtual

    def now(): VirtualTimestamp =
      fromMachine(TaggedTimestamp.machineNow())

  extension (a: ClockBase.type)
    inline def apply(
        baseMachineTimestamp: MachineTimestamp,
        baseVirtualTimestamp: VirtualTimestamp,
        clockSpeed: Double
    ): ClockBase =
      ClockBase(baseMachineTimestamp.untag, baseVirtualTimestamp.untag, clockSpeed)
