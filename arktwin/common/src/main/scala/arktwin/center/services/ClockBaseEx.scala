// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.services

import arktwin.common.data.DurationEx.*
import arktwin.common.data.Timestamp
import arktwin.common.data.TimestampEx.*

object ClockBaseEx:
  implicit class ClockBaseExtension(private val c: ClockBase) extends AnyVal:
    def fromMachine(machineTimestamp: Timestamp): Timestamp =
      c.baseTimestamp + (machineTimestamp - c.baseMachineTimestamp) * c.clockSpeed

    def now(): Timestamp =
      fromMachine(Timestamp.machineNow())
