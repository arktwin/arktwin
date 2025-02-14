// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.data.{TaggedTimestamp, Vector3Enu}
import arktwin.common.util.LoggerConfigurator.LogLevel
import arktwin.common.util.PureConfigHints.given
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import com.typesafe.config.{Config, ConfigRenderOptions}
import pureconfig.*
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.semiauto.{deriveReader, deriveWriter}

case class CenterConfig(
    dynamic: DynamicCenterConfig,
    static: StaticCenterConfig
) derives ConfigReader:
  def toJson: String =
    given ConfigWriter[CenterConfig] = deriveWriter
    ConfigWriter[CenterConfig].to(this).render(ConfigRenderOptions.concise())

  def validated(path: String): ValidatedNec[String, CenterConfig] =
    import cats.syntax.apply.*
    (
      dynamic.validated(s"$path.dynamic"),
      static.validated(s"$path.static")
    ).mapN(CenterConfig.apply)

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
      throw RuntimeException(
        (head +: tail).map(_.toString).mkString("config load error: ", ", ", "")
      )

  def loadOrThrow(): CenterConfig =
    given ConfigReader[CenterConfig] = deriveReader

    val path = "arktwin.center"
    configSource.at(path).loadOrThrow[CenterConfig].validated(path) match
      case Invalid(errors) =>
        throw RuntimeException(errors.toChain.toVector.mkString("config load error: ", ", ", ""))
      case Valid(config) =>
        config
