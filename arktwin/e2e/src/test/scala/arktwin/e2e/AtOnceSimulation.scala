// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.e2e

import arktwin.e2e.endpoints.*
import io.gatling.core.Predef.*
import io.gatling.http.Predef.*

class AtOnceSimulation extends Simulation:
  setUp(
    Seq(
      CenterClockSpeedPutScenario.builder,
      EdgeAgentsPostScenario.builder,
      EdgeConfigChartPutScenario.builder,
      EdgeConfigCoordinatePutScenario.builder
    ).map(_.inject(atOnceUsers(1))).reduceRight(_ andThen _)
  ).protocols(http.baseUrl("http://localhost:2237"))
    .assertions(global.failedRequests.count.is(0))
