// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.test

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpecLike

// A similar test utility also exists in arktwin.edge.test.
// We could share it in arktwin.common.test
// by using `compile->compile;test->test` as described in the following link:
// https://stackoverflow.com/questions/8193904/sbt-test-dependencies-in-multiprojects-make-the-test-code-available-to-dependen
// However, since some IDEs cannot resolve such dependencies, we have decided not to share it.
trait ActorTestBase extends ScalaTestWithActorTestKit with AnyFunSpecLike with BeforeAndAfterAll:
  override def beforeAll() =
    scribe.Logger.root.withMinimumLevel(scribe.Level.Error).replace()
