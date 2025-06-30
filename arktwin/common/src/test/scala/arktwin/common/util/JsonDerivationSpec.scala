// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.scalatest.funspec.AnyFunSpec

import scala.concurrent.duration.{FiniteDuration, SECONDS}

class JsonDerivationSpec extends AnyFunSpec:
  describe("JsonDerivation$"):
    describe("given JsonValueCodec[FiniteDuration]"):
      import arktwin.common.util.JsonDerivation.given

      it("serializes FiniteDuration to JSON string"):
        val duration = FiniteDuration(273, SECONDS)

        assert(writeToString(duration) == """"273 seconds"""")

      it("deserializes JSON string to FiniteDuration"):
        val json = """"273 seconds""""

        assert(readFromString[FiniteDuration](json) == FiniteDuration(273, SECONDS))

      it("throws JsonReaderException for invalid duration format"):
        val invalidJson = """"273 sseconds""""
        val exception = intercept[JsonReaderException]:
          readFromString[FiniteDuration](invalidJson)

        assert(
          exception.getMessage ==
            """expected '"', offset: 0x0000000d, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 22 32 37 33 20 73 73 65 63 6f 6e 64 73 22       | "273 sseconds"   |
            |+----------+-------------------------------------------------+------------------+""".stripMargin
        )

      it("throws JsonReaderException for infinite duration"):
        val infiniteJson = """"Inf""""
        val exception = intercept[JsonReaderException]:
          readFromString[FiniteDuration](infiniteJson)

        assert(
          exception.getMessage ==
            """expected '"', offset: 0x00000004, buf:
            |+----------+-------------------------------------------------+------------------+
            ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
            |+----------+-------------------------------------------------+------------------+
            || 00000000 | 22 49 6e 66 22                                  | "Inf"            |
            |+----------+-------------------------------------------------+------------------+""".stripMargin
        )
