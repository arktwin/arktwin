// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common

import pureconfig.generic.derivation.EnumConfigReader
import scribe.format.FormatBlock
import scribe.format.FormatBlock.Level.PaddedRight
import scribe.handler.SynchronousLogHandle
import scribe.mdc.MDC
import scribe.output.{Color, ColoredOutput, TextOutput}
import scribe.{Level, Logger}

object LoggerConfigurator:
  enum LogLevel derives EnumConfigReader:
    // Align with Pekko log levels
    // see org.apache.pekko.event.Logging.levelFor
    case Error, Warning, Info, Debug

  def init(minimumLevel: LogLevel, logLevelColor: Boolean): Unit =
    scribe.Logger.root
      .clearModifiers()
      .withMinimumLevel(minimumLevel match
        case LogLevel.Error   => Level.Error
        case LogLevel.Warning => Level.Warn
        case LogLevel.Info    => Level.Info
        case LogLevel.Debug   => Level.Debug
      )
      .clearHandlers()
      .withHandler(
        handle = SynchronousLogHandle,
        formatter =
          import scribe.format.*
          if logLevelColor then formatter"$dateFull $coloredLevelPaddedRight $messages   - $logSource"
          else formatter"$dateFull $levelPaddedRight $messages   - $logSource"
      )
      .replace()

  def coloredLevelPaddedRight: FormatBlock = FormatBlock: logRecord =>
    val output = PaddedRight.format(logRecord)
    logRecord.level match
      case Level.Warn  => ColoredOutput(Color.Yellow, output)
      case Level.Error => ColoredOutput(Color.Red, output)
      case _           => output

  def logSource: FormatBlock = FormatBlock: logRecord =>
    MDC.get("pekkoSource") match
      case Some(value) =>
        TextOutput(value.toString)
      case None =>
        TextOutput(logRecord.className + logRecord.methodName.map("." + _).getOrElse(""))
