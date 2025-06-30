// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import pureconfig.{ConfigCursor, ConfigReader, ConfigSource}

class EnumCaseInsensitiveConfigReaderSpec extends AnyFunSpec:
  enum TestEnum:
    case FirstValue
    case SecondValue
    case ThirdValue
  import TestEnum.*

  given reader: ConfigReader[TestEnum] = EnumCaseInsensitiveConfigReader(TestEnum.values)

  case class TestConfig(value: TestEnum) derives ConfigReader

  describe("EnumCaseInsensitiveConfigReader"):
    describe("from"):
      it("parses enum value with exact case"):
        val config = ConfigFactory.parseString("""value = "FirstValue"""")
        val result = ConfigSource.fromConfig(config).load[TestConfig]

        assert(result == Right(TestConfig(FirstValue)))

      it("parses enum value with lowercase"):
        val config = ConfigFactory.parseString("""value = "firstvalue"""")
        val result = ConfigSource.fromConfig(config).load[TestConfig]

        assert(result == Right(TestConfig(FirstValue)))

      it("parses enum value with uppercase"):
        val config = ConfigFactory.parseString("""value = "SECONDVALUE"""")
        val result = ConfigSource.fromConfig(config).load[TestConfig]

        assert(result == Right(TestConfig(SecondValue)))

      it("parses enum value with mixed case"):
        val config = ConfigFactory.parseString("""value = "tHiRdVaLuE"""")
        val result = ConfigSource.fromConfig(config).load[TestConfig]

        assert(result == Right(TestConfig(ThirdValue)))

      it("returns error for invalid enum value"):
        val config = ConfigFactory.parseString("""value = "InvalidValue"""")
        val result = ConfigSource.fromConfig(config).load[TestConfig]

        assert(result.isLeft)

      it("returns error for non-string value"):
        val config = ConfigFactory.parseString("value = 123")
        val result = ConfigSource.fromConfig(config).load[TestConfig]

        assert(result.isLeft)

      it("returns error for empty string value"):
        val config = ConfigFactory.parseString("""value = """"")
        val result = ConfigSource.fromConfig(config).load[TestConfig]

        assert(result.isLeft)
