// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}
import scribe.mdc.MDC

object BehaviorsExtensions:
  extension (a: Behaviors.type)
    // use Scribe mdc directly not via Pekko to reduce logging overhead
    def withScribeMdc[A](behavior: MDC ?=> Behavior[A]): Behavior[A] =
      MDC: mdc =>
        Behaviors.setup: context =>
          mdc("actor") = context.self.path.name
          behavior(using mdc)

    // use Scribe mdc directly not via Pekko to reduce logging overhead
    def setupWithScribeMdc[A](factory: MDC ?=> ActorContext[A] => Behavior[A]): Behavior[A] =
      MDC: mdc =>
        Behaviors.setup: context =>
          mdc("actor") = context.self.path.name
          factory(using mdc)(context)
