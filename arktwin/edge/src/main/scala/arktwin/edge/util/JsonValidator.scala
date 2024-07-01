// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import sttp.tapir.{Schema, ValidationResult, Validator}

import java.util.regex.PatternSyntaxException

object JsonValidator:
  def regex(schema: Schema[String]): Schema[String] = schema
    .description("java.util.regex.Pattern")
    .validate(
      Validator.custom(regex =>
        try
          regex.r
          ValidationResult.Valid
        catch
          case e: PatternSyntaxException =>
            ValidationResult.Invalid(List(e.getMessage))
      )
    )
