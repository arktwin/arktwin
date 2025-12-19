// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.common.data.*

trait ClockBaseTrait:
  val baseMachineTimestamp: Timestamp
  val baseTimestamp: Timestamp
  val clockSpeed: Double

  def fromMachine(machineTimestamp: MachineTimestamp): VirtualTimestamp =
    baseTimestamp.tagVirtual +
      ((machineTimestamp - baseMachineTimestamp.tagMachine) * clockSpeed).untag.tagVirtual

  def now(): VirtualTimestamp =
    fromMachine(TaggedTimestamp.machineNow())

trait ClockBaseCompanionTrait:
  def apply(
      baseMachineTimestamp: MachineTimestamp,
      baseVirtualTimestamp: VirtualTimestamp,
      clockSpeed: Double
  ): ClockBase =
    ClockBase(baseMachineTimestamp.untag, baseVirtualTimestamp.untag, clockSpeed)
