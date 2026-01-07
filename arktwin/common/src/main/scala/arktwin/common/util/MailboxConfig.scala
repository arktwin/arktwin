// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import org.apache.pekko.actor.typed.MailboxSelector

object MailboxConfig:
  def apply(target: Any): MailboxSelector =
    val relativePath = target.getClass.getName
    MailboxSelector.fromConfig(
      "pekko.actor.typed.mailbox."
        + (if relativePath.endsWith("$") then relativePath.dropRight(1) else relativePath)
    )
