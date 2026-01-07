// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import pureconfig.error.CannotConvert
import pureconfig.{ConfigCursor, ConfigReader}

class EnumCaseInsensitiveConfigReader[A](values: Array[A]) extends ConfigReader[A]:
  private val valuesMap: Map[String, A] = values.map(v => v.toString.toLowerCase -> v).toMap

  def from(cur: ConfigCursor): ConfigReader.Result[A] =
    cur.asString match
      case Right(str) =>
        valuesMap.get(str.toLowerCase) match
          case Some(value) =>
            Right(value)
          case None =>
            cur.failed(
              CannotConvert(
                str,
                "one of " + values.mkString(", "),
                "The value is not a valid enum option."
              )
            )

      case Left(error) => Left(error)
