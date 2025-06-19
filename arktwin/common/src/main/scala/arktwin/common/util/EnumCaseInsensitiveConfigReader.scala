// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import pureconfig.error.CannotConvert
import pureconfig.{ConfigCursor, ConfigReader}

class EnumCaseInsensitiveConfigReader[A](values: Array[A]) extends ConfigReader[A]:
  def from(cur: ConfigCursor): ConfigReader.Result[A] =
    cur.asString match
      case Right(str) =>
        values
          .find(_.toString.toLowerCase == str.toLowerCase)
          .map(Right(_))
          .getOrElse(
            cur.failed(
              CannotConvert(
                str,
                "one of " + values.mkString(", "),
                "The value is not a valid enum option."
              )
            )
          )
      case Left(error) => Left(error)
