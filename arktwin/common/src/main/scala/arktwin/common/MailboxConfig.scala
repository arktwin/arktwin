// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common

import org.apache.pekko.actor.typed.MailboxSelector

object MailboxConfig:
  def apply(relativePath: String): MailboxSelector =
    MailboxSelector.fromConfig(
      "pekko.actor.typed.mailbox."
        + (if relativePath.endsWith("$") then relativePath.dropRight(1) else relativePath)
    )
