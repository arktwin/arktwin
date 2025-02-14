// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

class ActorLogger(actorPath: String):
  private val prefix = s"[$actorPath] "
  def trace(message: => String): Unit = scribe.trace(prefix + message)
  def debug(message: => String): Unit = scribe.debug(prefix + message)
  def info(message: => String): Unit = scribe.info(prefix + message)
  def warn(message: => String): Unit = scribe.warn(prefix + message)
  def error(message: => String): Unit = scribe.error(prefix + message)
  def fatal(message: => String): Unit = scribe.fatal(prefix + message)
