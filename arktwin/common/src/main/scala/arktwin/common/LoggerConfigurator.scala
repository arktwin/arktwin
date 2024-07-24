// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common

import pureconfig.generic.derivation.EnumConfigReader
import scribe.format.FormatBlock
import scribe.handler.SynchronousLogHandle
import scribe.Logger
import scribe.LogRecord
import scribe.mdc.MDC
import scribe.output.LogOutput
import scribe.output.TextOutput

object LoggerConfigurator:
  def init(minimumLevel: LogLevel): Unit =
    scribe.Logger.root
      .clearModifiers()
      .withMinimumLevel(minimumLevel match
        case LogLevel.Error   => scribe.Level.Error
        case LogLevel.Warning => scribe.Level.Warn
        case LogLevel.Info    => scribe.Level.Info
        case LogLevel.Debug   => scribe.Level.Debug
      )
      .clearHandlers()
      .withHandler(
        handle = SynchronousLogHandle,
        formatter =
          import scribe.format.*
          formatter"$dateFull $levelPaddedRight $messages   - $LogSource"
      )
      .replace()

  enum LogLevel derives EnumConfigReader:
    // Align with Pekko log levels
    // see org.apache.pekko.event.Logging.levelFor
    case Error, Warning, Info, Debug

  object LogSource extends FormatBlock:
    override def format(record: LogRecord): LogOutput = MDC.get("pekkoSource") match
      case Some(value) =>
        TextOutput(value.toString)
      case None =>
        TextOutput(record.className + record.methodName.map("." + _).getOrElse(""))
