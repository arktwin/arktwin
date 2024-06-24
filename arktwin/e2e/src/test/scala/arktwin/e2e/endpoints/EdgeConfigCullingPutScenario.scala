/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.e2e.endpoints

import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*

import scala.concurrent.duration.DurationInt

object EdgeConfigCullingPutScenario:
  val builder: ScenarioBuilder =
    val cullingConfig =
      """{
        |  "edgeCulling": true,
        |  "maxFirstAgents": 123
        |}""".stripMargin.filterNot(_.isWhitespace)

    scenario(getClass.getSimpleName)
      .exec(
        http(_.scenario)
          .put("/api/edge/config/culling")
          .body(StringBody(cullingConfig))
          .check(status.is(202))
      )
      .pause(10.millis)
      .exec(
        http(_.scenario)
          .get("/api/edge/config")
          .check(status.is(200))
          .check(jsonPath("$.dynamic.culling").find.is(cullingConfig))
      )
