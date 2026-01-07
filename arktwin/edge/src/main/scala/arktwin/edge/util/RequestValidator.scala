// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import arktwin.edge.util.ErrorStatus.BadRequest
import scalapb.validate.*

object RequestValidator:
  def apply[A: Validator](value: A): Either[BadRequest, A] =
    Validator[A].validate(value) match
      case Success =>
        Right(value)
      case Failure(violations) =>
        Left(BadRequest(violations.map(toString)))

  def apply[A: Validator](values: Seq[A]): Either[BadRequest, Seq[A]] =
    val violations = for
      (value, i) <- values.zipWithIndex
      violation <- Validator[A].validate(value).toFailure.map(_.violations).getOrElse(Seq())
    yield violation.copy(field = s"[$i].${violation.field}")

    if violations.isEmpty then Right(values)
    else Left(BadRequest(violations.map(toString)))

  def apply[A: Validator](values: Map[String, A]): Either[BadRequest, Map[String, A]] =
    val violations = for
      (key, value) <- values.toSeq
      violation <- Validator[A].validate(value).toFailure.map(_.violations).getOrElse(Seq())
    yield violation.copy(field = s"$key.${violation.field}")

    if violations.isEmpty then Right(values)
    else Left(BadRequest(violations.map(toString)))

  private def toString(v: ValidationFailure): String =
    val fieldCamel = v.field
      .split('.')
      .map: s =>
        val ss = s.split('_')
        (ss.head +: ss.tail.map(s => s"${s.head.toUpper}${s.tail}")).mkString
      .mkString(".")
    s"$fieldCamel: ${v.reason} - Got ${v.value}"
