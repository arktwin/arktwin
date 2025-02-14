// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.{ActorContext, Behaviors}

object BehaviorsExtensions:
  extension (a: Behaviors.type)
    // use Scribe wrapper not via Pekko to reduce logging overhead
    def withLogger[A](behavior: ActorLogger => Behavior[A]): Behavior[A] =
      Behaviors.setup: context =>
        behavior(ActorLogger(context.self.path.name))

    // use Scribe wrapper not via Pekko to reduce logging overhead
    def setupWithLogger[A](factory: (ActorContext[A], ActorLogger) => Behavior[A]): Behavior[A] =
      Behaviors.setup: context =>
        factory(context, ActorLogger(context.self.path.name))
