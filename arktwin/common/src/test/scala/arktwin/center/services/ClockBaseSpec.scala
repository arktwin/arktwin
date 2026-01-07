// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.common.data.{MachineTimestamp, VirtualTimestamp}
import org.scalatest.funspec.AnyFunSpec

class ClockBaseSpec extends AnyFunSpec:
  describe("ClockBase"):
    describe("fromMachine"):
      it("converts machine time to virtual time with clock speed"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T01:01:01.111+09:00")
        val baseVirtual = VirtualTimestamp.parse("2000-01-01T01:02:01.111+09:00")
        val clockBase = ClockBase(baseMachine, baseVirtual, 1.5)
        val machineTime = MachineTimestamp.parse("2000-01-02T01:01:01.111+09:00")
        val result = clockBase.fromMachine(machineTime)

        // 24 hours at 1.5x speed = 36 hours virtual time elapsed
        assert(result == VirtualTimestamp.parse("2000-01-02T13:02:01.111+09:00"))

      it("returns base virtual time when machine time equals base machine time"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T00:00:00Z")
        val baseVirtual = VirtualTimestamp.parse("2000-02-01T12:00:00Z")
        val clockBase = ClockBase(baseMachine, baseVirtual, 2.0)
        val result = clockBase.fromMachine(baseMachine)

        assert(result == baseVirtual)

      it("handles zero clock speed"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T00:00:00Z")
        val baseVirtual = VirtualTimestamp.parse("2000-01-01T00:00:00Z")
        val clockBase = ClockBase(baseMachine, baseVirtual, 0.0)
        val machineTime = MachineTimestamp.parse("2000-01-02T00:00:00Z")
        val result = clockBase.fromMachine(machineTime)

        // With zero speed, virtual time stays at base
        assert(result == baseVirtual)

      it("handles negative clock speed"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T12:00:00Z")
        val baseVirtual = VirtualTimestamp.parse("2000-01-01T12:00:00Z")
        val clockBase = ClockBase(baseMachine, baseVirtual, -1.0)
        val machineTime = MachineTimestamp.parse("2000-01-01T13:00:00Z")
        val result = clockBase.fromMachine(machineTime)

        // 1 hour forward in machine time = 1 hour backward in virtual time
        assert(result == VirtualTimestamp.parse("2000-01-01T11:00:00Z"))

      it("handles fractional clock speed"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T00:00:00Z")
        val baseVirtual = VirtualTimestamp.parse("2000-01-01T00:00:00Z")
        val clockBase = ClockBase(baseMachine, baseVirtual, 0.5)
        val machineTime = MachineTimestamp.parse("2000-01-01T02:00:00Z")
        val result = clockBase.fromMachine(machineTime)

        // 2 hours at 0.5x speed = 1 hour virtual time
        assert(result == VirtualTimestamp.parse("2000-01-01T01:00:00Z"))

      it("handles past machine time"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T12:00:00Z")
        val baseVirtual = VirtualTimestamp.parse("2000-01-01T12:00:00Z")
        val clockBase = ClockBase(baseMachine, baseVirtual, 1.0)
        val machineTime = MachineTimestamp.parse("2000-01-01T10:00:00Z")
        val result = clockBase.fromMachine(machineTime)

        // 2 hours before base time
        assert(result == VirtualTimestamp.parse("2000-01-01T10:00:00Z"))

      it("handles very large clock speed"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T00:00:00Z")
        val baseVirtual = VirtualTimestamp.parse("2000-01-01T00:00:00Z")
        val clockBase = ClockBase(baseMachine, baseVirtual, 3600.0)
        val machineTime = MachineTimestamp.parse("2000-01-01T00:01:00Z")
        val result = clockBase.fromMachine(machineTime)

        // 1 minute at 3600x speed = 60 hours virtual time
        assert(result == VirtualTimestamp.parse("2000-01-03T12:00:00Z"))

      it("handles sub-second precision"):
        val baseMachine = MachineTimestamp.parse("2000-01-01T00:00:00.000Z")
        val baseVirtual = VirtualTimestamp.parse("2000-01-01T00:00:00.000Z")
        val clockBase = ClockBase(baseMachine, baseVirtual, 1.0)
        val machineTime = MachineTimestamp.parse("2000-01-01T00:00:00.500Z")
        val result = clockBase.fromMachine(machineTime)

        // 0.5 seconds later
        assert(result == VirtualTimestamp.parse("2000-01-01T00:00:00.500Z"))
