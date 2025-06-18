// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

object ErrorHandler:
  def flushAndExit(errorMessage: String): Unit =
    scribe.error(errorMessage)
    LoggerConfigurator.flush()
    sys.exit(1)
