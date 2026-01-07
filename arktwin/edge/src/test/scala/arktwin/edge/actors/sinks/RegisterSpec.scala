// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.{RegisterAgent, RegisterAgentDeleted, RegisterAgentUpdated}
import arktwin.edge.actors.sinks.Register.*
import arktwin.edge.test.ActorTestBase

class RegisterSpec extends ActorTestBase:
  describe("Register"):
    it("stores and updates agent registrations"):
      val register = testKit.spawn(Register())
      val reader = testKit.createTestProbe[ReadReply]()

      register ! Get(reader.ref)
      assert(reader.receiveMessage().value == Map())

      register ! Catch(RegisterAgent("a", "aaa", Map("x" -> "X"), Map("a" -> "A")))
      register ! Catch(RegisterAgent("b", "bbb", Map("x" -> "X"), Map("a" -> "B")))
      register ! Catch(RegisterAgent("c", "ccc", Map("x" -> "X"), Map("a" -> "C")))
      register ! Get(reader.ref)
      assert(
        reader.receiveMessage().value == Map(
          "a" -> RegisterAgent("a", "aaa", Map("x" -> "X"), Map("a" -> "A")),
          "b" -> RegisterAgent("b", "bbb", Map("x" -> "X"), Map("a" -> "B")),
          "c" -> RegisterAgent("c", "ccc", Map("x" -> "X"), Map("a" -> "C"))
        )
      )

      register ! Catch(RegisterAgentUpdated("b", Map("x" -> "XX")))
      register ! Catch(RegisterAgentUpdated("c", Map("y" -> "Y")))
      register ! Get(reader.ref)
      assert(
        reader.receiveMessage().value == Map(
          "a" -> RegisterAgent("a", "aaa", Map("x" -> "X"), Map("a" -> "A")),
          "b" -> RegisterAgent("b", "bbb", Map("x" -> "XX"), Map("a" -> "B")),
          "c" -> RegisterAgent("c", "ccc", Map("x" -> "X", "y" -> "Y"), Map("a" -> "C"))
        )
      )

      register ! Catch(RegisterAgentDeleted("a"))
      register ! Catch(RegisterAgentDeleted("b"))
      register ! Get(reader.ref)
      assert(
        reader.receiveMessage().value == Map(
          "c" -> RegisterAgent("c", "ccc", Map("x" -> "X", "y" -> "Y"), Map("a" -> "C"))
        )
      )
