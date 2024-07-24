// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.actors.sinks

import arktwin.center.services.{RegisterAgent, RegisterAgentDeleted, RegisterAgentUpdated}
import arktwin.edge.test.ActorTestBase

class RegisterSuite extends ActorTestBase:
  import Register.*

  test("Register"):
    val register = testKit.spawn(Register())
    val reader = testKit.createTestProbe[ReadReply]()

    register ! Read(reader.ref)
    reader.receiveMessage().value shouldBe Map()

    register ! Catch(RegisterAgent("a", "aaa", Map("x" -> "X"), Map("a" -> "A")))
    register ! Catch(RegisterAgent("b", "bbb", Map("x" -> "X"), Map("a" -> "B")))
    register ! Catch(RegisterAgent("c", "ccc", Map("x" -> "X"), Map("a" -> "C")))
    register ! Read(reader.ref)
    reader.receiveMessage().value shouldBe Map(
      "a" -> RegisterAgent("a", "aaa", Map("x" -> "X"), Map("a" -> "A")),
      "b" -> RegisterAgent("b", "bbb", Map("x" -> "X"), Map("a" -> "B")),
      "c" -> RegisterAgent("c", "ccc", Map("x" -> "X"), Map("a" -> "C"))
    )

    register ! Catch(RegisterAgentUpdated("b", Map("x" -> "XX")))
    register ! Catch(RegisterAgentUpdated("c", Map("y" -> "Y")))
    register ! Read(reader.ref)
    reader.receiveMessage().value shouldBe Map(
      "a" -> RegisterAgent("a", "aaa", Map("x" -> "X"), Map("a" -> "A")),
      "b" -> RegisterAgent("b", "bbb", Map("x" -> "XX"), Map("a" -> "B")),
      "c" -> RegisterAgent("c", "ccc", Map("x" -> "X", "y" -> "Y"), Map("a" -> "C"))
    )

    register ! Catch(RegisterAgentDeleted("a"))
    register ! Catch(RegisterAgentDeleted("b"))
    register ! Read(reader.ref)
    reader.receiveMessage().value shouldBe Map(
      "c" -> RegisterAgent("c", "ccc", Map("x" -> "X", "y" -> "Y"), Map("a" -> "C"))
    )
