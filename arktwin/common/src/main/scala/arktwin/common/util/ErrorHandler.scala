// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.util

object ErrorHandler:
  def flushAndExit(e: Throwable): Unit =
    scribe.error(e.getMessage())
    LoggerConfigurator.flush()
    sys.exit(1)
