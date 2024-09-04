// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.center.config

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
