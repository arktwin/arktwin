// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.e2e.endpoints

import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*

import scala.concurrent.duration.DurationInt

object EdgeConfigChartPutScenario:
  val builder: ScenarioBuilder =
    val chartConfig =
      """{
        |"culling":{
        |"enabled":true,
        |"maxFirstAgents":123
        |},
        |"expiration":{
        |"enabled":true,
        |"checkMachineInterval":"10 seconds",
        |"timeout":"20 seconds"
        |}
        |}""".stripMargin.linesIterator.mkString

    scenario(getClass.getSimpleName)
      .exec(
        http(_.scenario)
          .put("/api/edge/config/chart")
          .body(StringBody(chartConfig))
          .check(status.is(202))
      )
      .pause(10.millis)
      .exec(
        http(_.scenario)
          .get("/api/edge/config")
          .check(status.is(200))
          .check(jsonPath("$.dynamic.chart").find.is(chartConfig))
      )
