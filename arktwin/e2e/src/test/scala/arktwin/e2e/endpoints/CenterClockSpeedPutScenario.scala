// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.e2e.endpoints

import io.gatling.core.Predef.*
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef.*

object CenterClockSpeedPutScenario:
  val builder: ScenarioBuilder =
    scenario(getClass.getSimpleName)
      .exec(
        http(_.scenario)
          .put("/api/center/clock/speed")
          .body(StringBody("""{"clockSpeed": 1.2}"""))
          .check(status.is(202))
      )
