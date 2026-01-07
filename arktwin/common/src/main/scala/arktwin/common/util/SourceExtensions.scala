// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import org.apache.pekko.stream.scaladsl.*
import scribe.mdc.MDC

import scala.util.{Failure, Success}

object SourceExtensions:
  extension [Out, Mat](a: Source[Out, Mat])
    // call Scribe directly not via Pekko logger
    // Pekko logger evaluates unnecessary log messages because of call-by-value strategy
    def wireTapLog(streamName: String): Source[Out, Mat] =
      a.wireTap(
        Sink.combine(
          Sink.foreach[Out](a => scribe.trace(s"[$streamName] $a")),
          Flow[Out]
            .take(1)
            .to(Sink.foreach(_ => scribe.info(s"[$streamName] stream receives first element"))),
          Sink.onComplete[Out]:
            case Success(_) => scribe.warn(s"[$streamName] upstream completes")
            case Failure(e) => scribe.warn(s"[$streamName] downstream cancels, cause: $e")
        )(Broadcast(_))
      )
