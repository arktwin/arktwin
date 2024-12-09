// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import arktwin.common.data.TimestampEx.*
import arktwin.common.data.Vector3EnuEx.*

object TransformEnuEx:
  extension (a: TransformEnu)
    def extrapolate(targetVirtualTimestamp: VirtualTimestamp): TransformEnu =
      a.copy(
        timestamp = targetVirtualTimestamp.untag,
        localTranslationMeter = a.localTranslationMeter +
          (a.localTranslationSpeedMps * (targetVirtualTimestamp - a.timestamp.tagVirtual).secondsDouble)
      )
