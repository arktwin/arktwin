// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.common.data.*

trait ClockBaseTrait:
  val baseMachineTimestamp: MachineTimestamp
  val baseTimestamp: VirtualTimestamp
  val clockSpeed: Double

  def fromMachine(machineTimestamp: MachineTimestamp): VirtualTimestamp =
    val machineDuration = (machineTimestamp - baseMachineTimestamp)
    baseTimestamp + machineDuration.toVirtual(clockSpeed)

  def now(): VirtualTimestamp =
    fromMachine(MachineTimestamp.now())
