// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import pureconfig.ConfigFieldMapping
import pureconfig.generic.{FieldCoproductHint, ProductHint}

object PureConfigHints:
  given [A]: ProductHint[A] = ProductHint[A](
    ConfigFieldMapping(identity),
    useDefaultArgs = false,
    allowUnknownKeys = true
  )
  given [A]: FieldCoproductHint[A] = new FieldCoproductHint[A]("type"):
    override def fieldValue(name: String): String = name
