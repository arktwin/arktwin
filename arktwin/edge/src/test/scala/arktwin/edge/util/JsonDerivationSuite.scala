// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.{FiniteDuration, SECONDS}

class JsonDerivationSuite extends AnyFunSuite with Matchers:
  test("codecFiniteDuration"):
    import arktwin.edge.util.JsonDerivation.given

    readFromString[FiniteDuration]("\"273 seconds\"") shouldEqual FiniteDuration(273, SECONDS)
    writeToString(FiniteDuration(273, SECONDS)) shouldEqual "\"273 seconds\""

    intercept[JsonReaderException](
      readFromString[FiniteDuration]("\"273 sseconds\"")
    ).getMessage shouldEqual
      """expected '"', offset: 0x0000000d, buf:
        |+----------+-------------------------------------------------+------------------+
        ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
        |+----------+-------------------------------------------------+------------------+
        || 00000000 | 22 32 37 33 20 73 73 65 63 6f 6e 64 73 22       | "273 sseconds"   |
        |+----------+-------------------------------------------------+------------------+""".stripMargin

    intercept[JsonReaderException](
      readFromString[FiniteDuration]("\"Inf\"")
    ).getMessage shouldEqual
      """expected '"', offset: 0x00000004, buf:
        |+----------+-------------------------------------------------+------------------+
        ||          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |
        |+----------+-------------------------------------------------+------------------+
        || 00000000 | 22 49 6e 66 22                                  | "Inf"            |
        |+----------+-------------------------------------------------+------------------+""".stripMargin
