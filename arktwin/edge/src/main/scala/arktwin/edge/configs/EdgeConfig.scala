// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.center.services.CreateEdgeRequest
import arktwin.common.LoggerConfigurator.LogLevel
import com.typesafe.config.Config
import pureconfig.*
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.derivation.default.*
import scalapb.validate.{Failure, Validator}

import scala.concurrent.duration.FiniteDuration

case class EdgeConfig(
    dynamic: DynamicEdgeConfig,
    static: StaticEdgeConfig
) derives ConfigReader

object EdgeConfig:
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

  def loadOrThrow(): EdgeConfig =
    val config = configSource.at("arktwin.edge").loadOrThrow[EdgeConfig]
    Validator[CreateEdgeRequest].validate(CreateEdgeRequest(config.static.edgeIdPrefix)) match
      case Failure(violations) =>
        throw RuntimeException(
          "edge id prefix (arktwin.edge.static.edgeIdPrefix or ARKTWIN_EDGE_STATIC_EDGE_ID_PREFIX) " + violations
            .map(_.reason)
            .mkString(", ")
        )
      case _ =>
    config
