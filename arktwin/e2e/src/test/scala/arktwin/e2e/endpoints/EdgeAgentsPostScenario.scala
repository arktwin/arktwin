// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.e2e.endpoints

import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*

// TODO implement `GET /api/center/agents` to check results after `DELETE /api/center/agents`
object EdgeAgentsPostScenario:
  val builder: ScenarioBuilder =
    def requests(n: Int)(agentIdPrefix: String, kind: String): String = Seq
      .fill(n)(s"""{
        |  "agentIdPrefix": "$agentIdPrefix",
        |  "kind": "$kind",
        |  "status": {},
        |  "assets": {}
        |}""".stripMargin.filterNot(_.isWhitespace))
      .mkString("[", ",", "]")

    scenario(getClass.getSimpleName)
      .exec(
        http(_.scenario)
          .post("/api/edge/agents")
          .body(StringBody(requests(10)("-agent0", "-kind0")))
          .check(status.is(201))
      )
      .exec(
        http(_.scenario)
          .post("/api/edge/agents")
          .body(StringBody(requests(11)("-agent1", "-kind1")))
          .check(status.is(201))
      )
      .exec(
        http(_.scenario)
          .post("/api/edge/agents")
          .body(StringBody(requests(12)("-agent2", "-kind2")))
          .check(status.is(201))
      )
      .exec(
        http(_.scenario)
          .delete("/api/center/agents")
          .body(StringBody("""{"AgentIdSelector": {"regex": "-agent2-.+"}}"""))
          .check(status.is(202))
      )
      .exec(
        http(_.scenario)
          .delete("/api/center/agents")
          .body(StringBody("""{"AgentIdSelector": {"regex": "-kind1"}}"""))
          .check(status.is(202))
      )
      .exec(
        http(_.scenario)
          .delete("/api/center/agents")
          .body(StringBody("""{"AgentIdSelector": {"regex": "-kind.+"}}"""))
          .check(status.is(202))
      )
