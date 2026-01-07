// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.util

import arktwin.common.util.KamonFormatter.MetricLog
import kamon.tag.TagSet
import org.scalatest.funspec.AnyFunSpec

class KamonFormatterSpec extends AnyFunSpec:
  import KamonTagKeys.*
  import kamon.testkit.MetricSnapshotBuilder.*

  describe("KamonFormatter$"):
    describe("valueLogs"):
      it("formats counter metrics and sorts by name"):
        val result = KamonFormatter.valueLogs(
          Seq(
            counter(
              "gamma",
              TagSet
                .of(runIdKey, "run1")
                .withTag(recipientKey, "Actor1")
                .withTag(edgeIdKey, "edge1")
                .withTag(endpointKey, "endpoint1"),
              42
            ),
            counter(
              "beta",
              TagSet.of(runIdKey, "run2"),
              100
            ),
            counter(
              "alpha",
              TagSet
                .of(recipientKey, "Actor3")
                .withTag(edgeIdKey, "edge3")
                .withTag(runIdKey, "run3"),
              999
            ),
            counter(
              "delta",
              TagSet
                .of("extra", 1)
                .withTag(recipientKey, "Actor4")
                .withTag(edgeIdKey, "edge4")
                .withTag(runIdKey, "run4"),
              123
            )
          )
        )

        assert(
          result == Seq(
            MetricLog(
              "alpha",
              """alpha: 999 {recipient="Actor3", edge_id="edge3", run_id="run3"}"""
            ),
            MetricLog("beta", """beta: 100 {run_id="run2"}"""),
            MetricLog(
              "delta",
              """delta: 123 {recipient="Actor4", edge_id="edge4", run_id="run4", extra="1"}"""
            ),
            MetricLog(
              "gamma",
              """gamma: 42 {recipient="Actor1", endpoint="endpoint1", edge_id="edge1", run_id="run1"}"""
            )
          )
        )

      it("filters out zero values"):
        val result = KamonFormatter.valueLogs(
          Seq(
            counter("metric.a", TagSet.of(runIdKey, "run1"), 0),
            counter("metric.b", TagSet.of(runIdKey, "run1"), 10),
            counter("metric.c", TagSet.of(runIdKey, "run1"), 0)
          )
        )

        assert(result == Seq(MetricLog("metric.b", """metric.b: 10 {run_id="run1"}""")))

      it("handles empty metric list"):
        val result = KamonFormatter.valueLogs(Seq())

        assert(result.isEmpty)

    describe("distributionLogs"):
      import kamon.metric.MeasurementUnit.*

      it("formats histogram metrics and sorts by name"):
        val result = KamonFormatter.distributionLogs(
          Seq(
            histogram(
              "gamma",
              "",
              TagSet
                .of(runIdKey, "run1")
                .withTag(recipientKey, "Actor1")
                .withTag(edgeIdKey, "edge1")
                .withTag(endpointKey, "endpoint1"),
              time.milliseconds
            )(100, 200, 300),
            histogram(
              "beta",
              "",
              TagSet.of(runIdKey, "run2"),
              information.gigabytes
            )(50),
            histogram(
              "alpha",
              "",
              TagSet
                .of(recipientKey, "Actor3")
                .withTag(edgeIdKey, "edge3")
                .withTag(runIdKey, "run3"),
              none
            )(500, 600),
            histogram(
              "delta",
              "",
              TagSet
                .of("extra", 1)
                .withTag(recipientKey, "Actor4")
                .withTag(edgeIdKey, "edge4")
                .withTag(runIdKey, "run4")
            )(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
          )
        )

        assert(
          result == Seq(
            MetricLog(
              "alpha",
              """alpha: [500, 500, 500, 600, 600] {recipient="Actor3", edge_id="edge3", run_id="run3"}"""
            ),
            MetricLog("beta", """beta: [50, 50, 50, 50, 50] gigabytes {run_id="run2"}"""),
            MetricLog(
              "delta",
              """delta: [0, 5, 10, 15, 20] {recipient="Actor4", edge_id="edge4", run_id="run4", extra="1"}"""
            ),
            MetricLog(
              "gamma",
              """gamma: [100, 100, 200, 300, 300] milliseconds {recipient="Actor1", endpoint="endpoint1", edge_id="edge1", run_id="run1"}"""
            )
          )
        )

      it("filters out distributions with zero count"):
        val result = KamonFormatter.distributionLogs(
          Seq(
            histogram("metric.a", "", TagSet.of(runIdKey, "run1"), time.seconds)(),
            histogram("metric.b", "", TagSet.of(runIdKey, "run1"), time.seconds)(10),
            histogram("metric.c", "", TagSet.of(runIdKey, "run1"), time.seconds)()
          )
        )

        assert(
          result == Seq(
            MetricLog("metric.b", """metric.b: [10, 10, 10, 10, 10] seconds {run_id="run1"}""")
          )
        )

      it("handles empty metric list"):
        val result = KamonFormatter.distributionLogs(Seq())

        assert(result.isEmpty)
