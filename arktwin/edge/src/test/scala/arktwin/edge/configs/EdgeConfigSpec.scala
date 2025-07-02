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
          dynamic = DynamicEdgeConfig(
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
            ),
            culling = CullingConfig(
              edgeCulling = true,
              maxFirstAgents = 100
            )
          ),
          static = StaticEdgeConfig(
            edgeIdPrefix = "test",
            host = "localhost",
            port = 8081,
            portAutoIncrement = false,
            portAutoIncrementMax = 0,
            logLevel = LogLevel.Info,
            logLevelColor = true,
            logSuppressionList = Seq(),
            actorTimeout = 30.seconds,
            endpointTimeout = 10.seconds,
            clockInitialStashSize = 100,
            publishBatchSize = 50,
            publishBufferSize = 500
          )
        )
        val json = config.toJson

        assert(
          json == """{"dynamic":{"coordinate":{"axis":{"xDirection":{"type":"East"},"yDirection":{"type":"North"},"zDirection":{"type":"Up"}},"centerOrigin":{"x":0.0,"y":0.0,"z":0.0},"lengthUnit":{"type":"Meter"},"rotation":{"type":"QuaternionConfig"},"speedUnit":{"type":"MeterPerSecond"}},"culling":{"edgeCulling":true,"maxFirstAgents":100}},"static":{"actorTimeout":"30s","clockInitialStashSize":100,"edgeIdPrefix":"test","endpointTimeout":"10s","host":"localhost","logLevel":{"type":"Info"},"logLevelColor":true,"logSuppressionList":[],"port":8081,"portAutoIncrement":false,"portAutoIncrementMax":0,"publishBatchSize":50,"publishBufferSize":500}}"""
        )

    describe("validated"):
      it("returns valid config when all fields are valid"):
        val config = EdgeConfig(
          dynamic = DynamicEdgeConfig(
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
            ),
            culling = CullingConfig(
              edgeCulling = false,
              maxFirstAgents = 50
            )
          ),
          static = StaticEdgeConfig(
            edgeIdPrefix = "valid",
            host = "0.0.0.0",
            port = 9090,
            portAutoIncrement = true,
            portAutoIncrementMax = 10,
            logLevel = LogLevel.Debug,
            logLevelColor = false,
            logSuppressionList = Seq("org.apache.pekko"),
            actorTimeout = 60.seconds,
            endpointTimeout = 20.seconds,
            clockInitialStashSize = 200,
            publishBatchSize = 100,
            publishBufferSize = 1000
          )
        )

        assert(config.validated("test").isValid)

      it("returns invalid with error messages when fields are invalid"):
        val config = EdgeConfig(
          dynamic = DynamicEdgeConfig(
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
            ),
            culling = CullingConfig(
              edgeCulling = true,
              maxFirstAgents = -1
            )
          ),
          static = StaticEdgeConfig(
            edgeIdPrefix = "",
            host = "",
            port = 0,
            portAutoIncrement = false,
            portAutoIncrementMax = -1,
            logLevel = LogLevel.Error,
            logLevelColor = true,
            logSuppressionList = Seq(),
            actorTimeout = 0.seconds,
            endpointTimeout = 0.seconds,
            clockInitialStashSize = 0,
            publishBatchSize = 0,
            publishBufferSize = 0
          )
        )
        val result = config.validated("test")

        assert(result.isInvalid)
        assert(
          result.swap.getOrElse(NonEmptyChain.one("")).toChain.toVector.toSet == Set(
            "test.dynamic.coordinate.axis.{x, y, z} must contain (East or West) and (North or South) and (Up or Down)",
            "test.dynamic.culling.maxFirstAgents must be >= 0",
            "test.static.edgeIdPrefix: must match pattern [0-9a-zA-Z\\-_]+",
            "test.static.host must not be empty",
            "test.static.port must be > 0",
            "test.static.portAutoIncrementMax must be >= 0",
            "test.static.actorTimeout must be > 0",
            "test.static.endpointTimeout must be > 0",
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
