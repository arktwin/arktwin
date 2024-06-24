/*
 * Copyright 2024 TOYOTA MOTOR CORPORATION
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package arktwin.e2e

import arktwin.e2e.endpoints.*
import io.gatling.core.Predef.*
import io.gatling.http.Predef.*

class AtOnceSimulation extends Simulation:
  setUp(
    Seq(
      CenterClockSpeedPutScenario.builder,
      EdgeAgentsPostScenario.builder,
      EdgeConfigCoordinatePutScenario.builder,
      EdgeConfigCullingPutScenario.builder
    ).map(_.inject(atOnceUsers(1))).reduceRight(_ andThen _)
  ).protocols(http.baseUrl("http://localhost:2237"))
