// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.configs

import arktwin.common.util.LoggerConfigurator.LogLevel
import arktwin.edge.data.Vector3
import cats.data.NonEmptyChain
import org.scalatest.funspec.AnyFunSpec

import scala.concurrent.duration.DurationInt

class EdgeConfigSpec extends AnyFunSpec:
  describe("EdgeConfig"):
    describe("toJson"):
      it("converts config to JSON string"):
        val config = EdgeConfig(
          dynamic = EdgeDynamicConfig(
            chart = ChartConfig(
              culling = true,
              cullingMaxFirstAgents = 100,
              expiration = false,
              expirationCheckMachineInterval = 1.second,
              expirationTimeout = 3.seconds
            ),
            coordinate = CoordinateConfig(
              axis = AxisConfig(
                xDirection = AxisConfig.Direction.East,
                yDirection = AxisConfig.Direction.North,
                zDirection = AxisConfig.Direction.Up
              ),
              centerOrigin = Vector3(0, 0, 0),
              rotation = QuaternionConfig,
              lengthUnit = CoordinateConfig.LengthUnit.Meter,
              speedUnit = CoordinateConfig.SpeedUnit.MeterPerSecond
            )
          ),
          static = EdgeStaticConfig(
            actorMachineTimeout = 30.seconds,
            clockInitialStashSize = 100,
            edgeIdPrefix = "test",
            endpointMachineTimeout = 10.seconds,
            host = "localhost",
            port = 8081,
            portAutoIncrement = false,
            portAutoIncrementMax = 0,
            logLevel = LogLevel.Info,
            logLevelColor = true,
            logSuppressionList = Seq(),
            publishBatchSize = 50,
            publishBufferSize = 500
          )
        )
        val json = config.toJson

        assert(
          json == """{"dynamic":{"chart":{"culling":true,"cullingMaxFirstAgents":100,"expiration":false,"expirationCheckMachineInterval":"1s","expirationTimeout":"3s"},"coordinate":{"axis":{"xDirection":{"type":"East"},"yDirection":{"type":"North"},"zDirection":{"type":"Up"}},"centerOrigin":{"x":0.0,"y":0.0,"z":0.0},"lengthUnit":{"type":"Meter"},"rotation":{"type":"QuaternionConfig"},"speedUnit":{"type":"MeterPerSecond"}}},"static":{"actorMachineTimeout":"30s","clockInitialStashSize":100,"edgeIdPrefix":"test","endpointMachineTimeout":"10s","host":"localhost","logLevel":{"type":"Info"},"logLevelColor":true,"logSuppressionList":[],"port":8081,"portAutoIncrement":false,"portAutoIncrementMax":0,"publishBatchSize":50,"publishBufferSize":500}}"""
        )

    describe("validated"):
      it("returns valid config when all fields are valid"):
        val config = EdgeConfig(
          dynamic = EdgeDynamicConfig(
            chart = ChartConfig(
              culling = false,
              cullingMaxFirstAgents = 50,
              expiration = true,
              expirationCheckMachineInterval = 10.second,
              expirationTimeout = 20.seconds
            ),
            coordinate = CoordinateConfig(
              axis = AxisConfig(
                xDirection = AxisConfig.Direction.East,
                yDirection = AxisConfig.Direction.North,
                zDirection = AxisConfig.Direction.Up
              ),
              centerOrigin = Vector3(1, 2, 3),
              rotation = EulerAnglesConfig(
                angleUnit = EulerAnglesConfig.AngleUnit.Degree,
                rotationMode = EulerAnglesConfig.RotationMode.Extrinsic,
                rotationOrder = EulerAnglesConfig.RotationOrder.XYZ
              ),
              lengthUnit = CoordinateConfig.LengthUnit.Millimeter,
              speedUnit = CoordinateConfig.SpeedUnit.KilometerPerHour
            )
          ),
          static = EdgeStaticConfig(
            actorMachineTimeout = 60.seconds,
            clockInitialStashSize = 200,
            edgeIdPrefix = "valid",
            endpointMachineTimeout = 20.seconds,
            host = "0.0.0.0",
            port = 9090,
            portAutoIncrement = true,
            portAutoIncrementMax = 10,
            logLevel = LogLevel.Debug,
            logLevelColor = false,
            logSuppressionList = Seq("org.apache.pekko"),
            publishBatchSize = 100,
            publishBufferSize = 1000
          )
        )

        assert(config.validated("test").isValid)

      it("returns invalid with error messages when fields are invalid"):
        val config = EdgeConfig(
          dynamic = EdgeDynamicConfig(
            chart = ChartConfig(
              culling = true,
              cullingMaxFirstAgents = -1,
              expiration = false,
              expirationCheckMachineInterval = -1.second,
              expirationTimeout = -3.seconds
            ),
            coordinate = CoordinateConfig(
              axis = AxisConfig(
                xDirection = AxisConfig.Direction.East,
                yDirection = AxisConfig.Direction.East,
                zDirection = AxisConfig.Direction.East
              ),
              centerOrigin = Vector3(0, 0, 0),
              rotation = QuaternionConfig,
              lengthUnit = CoordinateConfig.LengthUnit.Meter,
              speedUnit = CoordinateConfig.SpeedUnit.MeterPerSecond
            )
          ),
          static = EdgeStaticConfig(
            actorMachineTimeout = 0.seconds,
            clockInitialStashSize = 0,
            edgeIdPrefix = "",
            endpointMachineTimeout = 0.seconds,
            host = "",
            port = 0,
            portAutoIncrement = false,
            portAutoIncrementMax = -1,
            logLevel = LogLevel.Error,
            logLevelColor = true,
            logSuppressionList = Seq(),
            publishBatchSize = 0,
            publishBufferSize = 0
          )
        )
        val result = config.validated("test")

        assert(result.isInvalid)
        assert(
          result.swap.getOrElse(NonEmptyChain.one("")).toChain.toVector.toSet == Set(
            "test.dynamic.coordinate.axis.{x, y, z} must contain (East or West) and (North or South) and (Up or Down)",
            "test.dynamic.chart.cullingMaxFirstAgents must be >= 0",
            "test.dynamic.chart.expirationCheckMachineInterval must be > 0",
            "test.dynamic.chart.expirationTimeout must be > 0",
            "test.static.edgeIdPrefix: must match pattern [0-9a-zA-Z\\-_]+",
            "test.static.host must not be empty",
            "test.static.port must be > 0",
            "test.static.portAutoIncrementMax must be >= 0",
            "test.static.actorMachineTimeout must be > 0",
            "test.static.endpointMachineTimeout must be > 0",
            "test.static.clockInitialStashSize must be > 0",
            "test.static.publishBatchSize must be > 0",
            "test.static.publishBufferSize must be > 0"
          )
        )

  describe("EdgeConfig$"):
    describe("loadRawOrThrow"):
      it("loads and resolves Config from actual config files without exceptions"):
        EdgeConfig.loadRawOrThrow()

    describe("loadOrThrow"):
      it("loads and validates EdgeConfig from actual config files without exceptions"):
        EdgeConfig.loadOrThrow()
