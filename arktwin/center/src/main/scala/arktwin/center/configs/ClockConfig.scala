// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.data.{MachineDuration, VirtualDuration, VirtualTimestamp}

case class ClockConfig(
    start: ClockConfig.Start
)

object ClockConfig:
  case class Start(
      initialTime: Start.InitialTime,
      clockSpeed: Double,
      condition: Start.Condition
  )

  object Start:
    sealed trait InitialTime
    case class Absolute(absolute: VirtualTimestamp) extends InitialTime
    case class Relative(relative: VirtualDuration) extends InitialTime
    sealed trait Condition
    case class Schedule(schedule: MachineDuration) extends Condition
    // TODO case class Agents(agents: Map[String, Int], agentsCheckInterval: MachineDuration) extends Condition
