// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center

import arktwin.common.LoggerConfigurator.LogLevel
import arktwin.common.data.{Duration, Timestamp, Vector3Enu}
import com.typesafe.config.Config
import pureconfig.*
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.derivation.default.*

import scala.concurrent.duration.FiniteDuration

case class CenterConfig(
    dynamic: DynamicCenterConfig,
    static: StaticCenterConfig
) derives ConfigReader

object CenterConfig:
  private val configSource = ConfigSource.defaultOverrides
    .withFallback(ConfigSource.defaultApplication) // -Dconfig.file
    .withFallback(ConfigSource.resources("pekko.conf"))
    .withFallback(ConfigSource.resources("kamon.conf"))
    .withFallback(
      ConfigSource.defaultReference
    ) // merged reference.conf from Pekko, Kamon and ArkTwin

  def loadRawOrThrow(): Config = configSource.config() match
    case Right(value) =>
      value.resolve()
    case Left(ConfigReaderFailures(head, tail*)) =>
      throw RuntimeException((head +: tail).map(_.toString).mkString(", "))

  def loadOrThrow(): CenterConfig =
    configSource.at("arktwin.center").loadOrThrow[CenterConfig]

case class StaticCenterConfig(
    clock: StaticCenterConfig.ClockConfig,
    runIdPrefix: String,
    host: String,
    port: Int,
    logLevel: LogLevel,
    logLevelColor: Boolean,
    subscribeBatchSize: Int,
    subscribeBatchInterval: FiniteDuration,
    subscribeBufferSize: Int
)

// TODO changeable via Admin API
case class DynamicCenterConfig(
    atlas: DynamicCenterConfig.AtlasConfig
)

object DynamicCenterConfig:
  case class AtlasConfig(
      culling: AtlasConfig.Culling,
      interval: FiniteDuration
  )

  object AtlasConfig:
    sealed trait Culling
    case class Broadcast() extends Culling
    case class GridCulling(gridCellSize: Vector3Enu) extends Culling

object StaticCenterConfig:
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
      case class Absolute(absolute: Timestamp) extends InitialTime
      case class Relative(relative: Duration) extends InitialTime
      sealed trait Condition
      case class Schedule(schedule: Duration) extends Condition
      // TODO case class Agents(agents: Map[String, Int], agentsCheckInterval: Duration) extends Condition
