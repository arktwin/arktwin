// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.test

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike

trait ActorTestBase extends ScalaTestWithActorTestKit with AnyFunSuiteLike with BeforeAndAfterAll:
  override def beforeAll() =
    scribe.Logger.root.withMinimumLevel(scribe.Level.Error).replace()
