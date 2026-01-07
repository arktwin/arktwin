// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.scalatest.funspec.AnyFunSpec

class EnumCaseInsensitiveJsonValueCodecSpec extends AnyFunSpec:
  enum TestEnum:
    case FirstValue
    case SecondValue
    case ThirdValue
  import TestEnum.*

  given JsonValueCodec[TestEnum] = EnumCaseInsensitiveJsonValueCodec(values, FirstValue)

  describe("EnumCaseInsensitiveJsonValueCodec"):
    describe("decodeValue"):
      it("reads enum value with exact case"):
        assert(readFromString[TestEnum](""""FirstValue"""") == FirstValue)

      it("reads enum value with lowercase"):
        assert(readFromString[TestEnum](""""firstvalue"""") == FirstValue)

      it("reads enum value with uppercase"):
        assert(readFromString[TestEnum](""""SECONDVALUE"""") == SecondValue)

      it("reads enum value with mixed case"):
        assert(readFromString[TestEnum](""""tHiRdVaLuE"""") == ThirdValue)

      it("throws IllegalArgumentException for invalid enum value"):
        intercept[IllegalArgumentException]:
          readFromString[TestEnum](""""InvalidValue"""")

      it("throws JsonReaderException for non-string value"):
        intercept[JsonReaderException]:
          readFromString[TestEnum]("123")

      it("throws IllegalArgumentException for empty string"):
        intercept[IllegalArgumentException]:
          readFromString[TestEnum]("""""""")

    describe("encodeValue"):
      it("outputs enum values with exact case"):
        assert(writeToString(FirstValue) == """"FirstValue"""")
        assert(writeToString(SecondValue) == """"SecondValue"""")
        assert(writeToString(ThirdValue) == """"ThirdValue"""")
