// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}

class EnumCaseInsensitiveJsonValueCodec[A](values: Array[A], override val nullValue: A)
    extends JsonValueCodec[A]:
  private val valuesMap = values.map(v => v.toString.toLowerCase -> v).toMap

  override def decodeValue(in: JsonReader, default: A): A =
    val str = in.readString("")
    valuesMap.get(str.toLowerCase) match
      case Some(value) => value
      case None        =>
        throw new IllegalArgumentException(
          s"Unknown enum value: $str. Expected one of ${values.mkString(", ")}."
        )

  override def encodeValue(x: A, out: JsonWriter): Unit =
    out.writeVal(x.toString)
