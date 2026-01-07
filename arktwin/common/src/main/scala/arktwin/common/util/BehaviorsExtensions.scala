// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object BehaviorsExtensions:
  extension (a: Behaviors.type)
    // call Scribe wrapper not via Pekko logger
    // Pekko logger evaluates unnecessary log messages because of call-by-value strategy
    def withLogger[A](behavior: ActorLogger => Behavior[A]): Behavior[A] =
      Behaviors.setup: context =>
        behavior(ActorLogger(context.self.path.toString))

    // call Scribe wrapper not via Pekko logger
    // Pekko logger evaluates unnecessary log messages because of call-by-value strategy
    def setupWithLogger[A](factory: (ActorContext[A], ActorLogger) => Behavior[A]): Behavior[A] =
      Behaviors.setup: context =>
        factory(context, ActorLogger(context.self.path.toString))
