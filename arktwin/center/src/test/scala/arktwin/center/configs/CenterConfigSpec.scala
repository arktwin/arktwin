// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.center.configs

import arktwin.common.data.*
import arktwin.common.util.LoggerConfigurator.LogLevel
import cats.data.NonEmptyChain
import org.scalatest.funspec.AnyFunSpec

import scala.concurrent.duration.DurationInt

class CenterConfigSpec extends AnyFunSpec:
  describe("CenterConfig"):
    describe("toJson"):
      it("converts config to JSON string"):
        val config = CenterConfig(
          dynamic = DynamicCenterConfig(
            atlas = AtlasConfig(
              culling = AtlasConfig.Broadcast(),
              routeTableUpdateMachineInterval = 1.second
            )
          ),
          static = StaticCenterConfig(
            clock = ClockConfig(
              start = ClockConfig.Start(
                initialTime = ClockConfig.Start.Relative(VirtualDuration(0, 0)),
                clockSpeed = 1.0,
                condition = ClockConfig.Start.Schedule(MachineDuration(0, 0))
              )
            ),
            runIdPrefix = "test",
            host = "localhost",
            port = 8080,
            portAutoIncrement = false,
            portAutoIncrementMax = 0,
            logLevel = LogLevel.Info,
            logLevelColor = true,
            logSuppressionList = Seq(),
            actorMachineTimeout = 30.seconds,
            subscribeBatchSize = 100,
            subscribeBatchMachineInterval = 100.millis,
            subscribeBufferSize = 1000
          )
        )
        val json = config.toJson

        assert(
          json == """{"dynamic":{"atlas":{"culling":{"type":"Broadcast"},"routeTableUpdateMachineInterval":"1s"}},"static":{"actorMachineTimeout":"30s","clock":{"start":{"clockSpeed":1.0,"condition":{"schedule":{"nanos":0,"seconds":0},"type":"Schedule"},"initialTime":{"relative":{"nanos":0,"seconds":0},"type":"Relative"}}},"host":"localhost","logLevel":{"type":"Info"},"logLevelColor":true,"logSuppressionList":[],"port":8080,"portAutoIncrement":false,"portAutoIncrementMax":0,"runIdPrefix":"test","subscribeBatchMachineInterval":"100ms","subscribeBatchSize":100,"subscribeBufferSize":1000}}"""
        )

    describe("validated"):
      it("returns valid config when all fields are valid"):
        val config = CenterConfig(
          dynamic = DynamicCenterConfig(
            atlas = AtlasConfig(
              culling = AtlasConfig.GridCulling(Vector3Enu(1, 1, 1)),
              routeTableUpdateMachineInterval = 1.second
            )
          ),
          static = StaticCenterConfig(
            clock = ClockConfig(
              start = ClockConfig.Start(
                initialTime = ClockConfig.Start.Absolute(VirtualTimestamp(0, 0)),
                clockSpeed = 1.0,
                condition = ClockConfig.Start.Schedule(MachineDuration(100, 0))
              )
            ),
            runIdPrefix = "valid",
            host = "localhost",
            port = 8080,
            portAutoIncrement = true,
            portAutoIncrementMax = 10,
            logLevel = LogLevel.Debug,
            logLevelColor = false,
            logSuppressionList = Seq("org.apache.pekko"),
            actorMachineTimeout = 60.seconds,
            subscribeBatchSize = 50,
            subscribeBatchMachineInterval = 200.millis,
            subscribeBufferSize = 500
          )
        )

        assert(config.validated("test").isValid)

      it("returns invalid with error messages when fields are invalid"):
        val config = CenterConfig(
          dynamic = DynamicCenterConfig(
            atlas = AtlasConfig(
              culling = AtlasConfig.GridCulling(Vector3Enu(0, -1, 1)),
              routeTableUpdateMachineInterval = 0.seconds
            )
          ),
          static = StaticCenterConfig(
            clock = ClockConfig(
              start = ClockConfig.Start(
                initialTime = ClockConfig.Start.Relative(VirtualDuration(0, 0)),
                clockSpeed = -1.0,
                condition = ClockConfig.Start.Schedule(MachineDuration(-1, 0))
              )
            ),
            runIdPrefix = "",
            host = "",
            port = 0,
            portAutoIncrement = false,
            portAutoIncrementMax = -1,
            logLevel = LogLevel.Error,
            logLevelColor = true,
            logSuppressionList = Seq(),
            actorMachineTimeout = 0.seconds,
            subscribeBatchSize = 0,
            subscribeBatchMachineInterval = 0.seconds,
            subscribeBufferSize = 0
          )
        )
        val result = config.validated("test")

        assert(result.isInvalid)
        assert(
          result.swap.getOrElse(NonEmptyChain.one("")).toChain.toVector.toSet == Set(
            "test.dynamic.atlas.culling.gridCellSize.{x, y, z} must be > 0",
            "test.dynamic.atlas.routeTableUpdateMachineInterval must be > 0",
            "test.static.clock.start.clockSpeed must be >= 0",
            "test.static.clock.start.condition.schedule must be >= 0",
            "test.static.runIdPrefix must not be empty",
            "test.static.host must not be empty",
            "test.static.port must be > 0",
            "test.static.portAutoIncrementMax must be >= 0",
            "test.static.actorMachineTimeout must be > 0",
            "test.static.subscribeBatchSize must be > 0",
            "test.static.subscribeBatchMachineInterval must be > 0",
            "test.static.subscribeBufferSize must be > 0"
          )
        )

  describe("CenterConfig$"):
    describe("loadRawOrThrow"):
      it("loads and resolves Config from actual config files without exceptions"):
        CenterConfig.loadRawOrThrow()

    describe("loadOrThrow"):
      it("loads and validates CenterConfig from actual config files without exceptions"):
        CenterConfig.loadOrThrow()
