// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.e2e.endpoints

import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*

import scala.concurrent.duration.DurationInt

object EdgeConfigCoordinatePutScenario:
  val builder: ScenarioBuilder =
    val coordinateConfig =
      """{
      |  "axis": {
      |    "xDirection": "North",
      |    "yDirection": "East",
      |    "zDirection": "Down"
      |  },
      |  "centerOrigin": {
      |    "x": 1.1,
      |    "y": 2.3,
      |    "z": 4.5
      |  },
      |  "lengthUnit": "Millimeter",
      |  "rotation": {
      |    "EulerAnglesConfig": {
      |      "angleUnit": "Degree",
      |      "rotationMode": "Extrinsic",
      |      "rotationOrder": "ZXY"
      |    }
      |  },
      |  "speedUnit": "KilometerPerHour"
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
