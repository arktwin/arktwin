// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.test

import org.apache.pekko.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import org.scalatest.funspec.AnyFunSpecLike

import scala.util.Using.Releasable

trait ActorTestBase extends ScalaTestWithActorTestKit with AnyFunSpecLike:
  given Releasable[ActorTestKit] = _.shutdownTestKit()

  override def beforeAll() =
    scribe.Logger.root.withMinimumLevel(scribe.Level.Error).replace()
