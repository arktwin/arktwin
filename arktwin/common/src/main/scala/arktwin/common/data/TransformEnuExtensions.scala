// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.data.TimestampExtensions.*
import arktwin.common.data.Vector3EnuExtensions.*

object TransformEnuExtensions:
  extension (a: TransformEnu)
    def extrapolate(targetVirtualTimestamp: VirtualTimestamp): TransformEnu =
      a.copy(
        timestamp = targetVirtualTimestamp.untag,
        localTranslationMeter = a.localTranslationMeter +
          (a.localTranslationSpeedMps * (targetVirtualTimestamp - a.timestamp.tagVirtual).secondsDouble)
      )
