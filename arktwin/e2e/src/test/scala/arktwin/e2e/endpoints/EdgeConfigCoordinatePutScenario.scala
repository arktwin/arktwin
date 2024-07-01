// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.e2e.endpoints

import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*

import scala.concurrent.duration.DurationInt

object EdgeConfigCoordinatePutScenario:
  val builder: ScenarioBuilder =
    val coordinateConfig =
      """{
      |  "vector3": {
      |    "lengthUnit": "Meter",
      |    "speedUnit": "Second",
      |    "x": "East",
      |    "y": "North",
      |    "z": "Down"
      |  },
      |  "rotation": {
      |    "EulerAnglesConfig": {
      |      "angleUnit": "Degree",
      |      "order": "XYZ"
      |    }
      |  }
      |}""".stripMargin.filterNot(_.isWhitespace)

    scenario(getClass.getSimpleName)
      .exec(
        http(_.scenario)
          .put("/api/edge/config/coordinate")
          .body(StringBody(coordinateConfig))
          .check(status.is(202))
      )
      .pause(10.millis)
      .exec(
        http(_.scenario)
          .get("/api/edge/config")
          .check(status.is(200))
          .check(jsonPath("$.dynamic.coordinate").find.is(coordinateConfig))
      )
