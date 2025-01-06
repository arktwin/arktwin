// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.data.{MachineDuration, VirtualDuration, VirtualTimestamp}
import cats.data.Validated.{condNec, valid}
import cats.data.ValidatedNec

case class ClockConfig(
    start: ClockConfig.Start
):
  def validated(path: String): ValidatedNec[String, ClockConfig] =
    start.validated(s"$path.start").map(ClockConfig.apply)

object ClockConfig:
  case class Start(
      initialTime: Start.InitialTime,
      clockSpeed: Double,
      condition: Start.Condition
  ):
    def validated(path: String): ValidatedNec[String, Start] =
      import cats.syntax.apply.*
      (
        initialTime.validated(s"$path.initialTime"),
        condNec(
          clockSpeed >= 0,
          clockSpeed,
          s"$path.clockSpeed must be >= 0"
        ),
        condition.validated(s"$path.condition")
      ).mapN(Start.apply)

  object Start:
    sealed trait InitialTime:
      def validated(path: String): ValidatedNec[String, InitialTime]

    case class Absolute(absolute: VirtualTimestamp) extends InitialTime:
      override def validated(path: String): ValidatedNec[String, Absolute] =
        valid(this)

    case class Relative(relative: VirtualDuration) extends InitialTime:
      override def validated(path: String): ValidatedNec[String, Relative] =
        valid(this)

    sealed trait Condition:
      def validated(path: String): ValidatedNec[String, Condition]

    case class Schedule(schedule: MachineDuration) extends Condition:
      override def validated(path: String): ValidatedNec[String, Schedule] =
        condNec(
          schedule >= MachineDuration(0, 0),
          schedule,
          s"$path.schedule must be >= 0"
        ).map(Schedule.apply)
    // TODO case class Agents(agents: Map[String, Int], agentsCheckInterval: MachineDuration) extends Condition
