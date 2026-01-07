// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2026 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class Vector3EnuSpec extends AnyFunSpec with Matchers:
  given Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-6)
  given Equality[Vector3Enu] =
    case (a: Vector3Enu, b: Vector3Enu) =>
      Seq(a.x, a.y, a.z).zip(Seq(b.x, b.y, b.z)).forall(summon[Equality[Double]].areEqual.tupled)
    case _ =>
      false

  describe("Vector3Enu"):
    describe("+"):
      it("adds two vectors"):
        val v1 = Vector3Enu(1.0, 2.0, 3.0)
        val v2 = Vector3Enu(4.0, 5.0, 6.0)

        assert(v1 + v2 === Vector3Enu(5.0, 7.0, 9.0))

      it("adds two vectors with negative values"):
        val v1 = Vector3Enu(-1.0, 2.0, -3.0)
        val v2 = Vector3Enu(4.0, -5.0, 6.0)

        assert(v1 + v2 === Vector3Enu(3.0, -3.0, 3.0))

    describe("-"):
      it("subtracts two vectors"):
        val v1 = Vector3Enu(5.0, 7.0, 9.0)
        val v2 = Vector3Enu(1.0, 2.0, 3.0)

        assert(v1 - v2 === Vector3Enu(4.0, 5.0, 6.0))

      it("subtracts two vectors resulting in negative values"):
        val v1 = Vector3Enu(1.0, 2.0, 3.0)
        val v2 = Vector3Enu(4.0, 5.0, 6.0)

        assert(v1 - v2 === Vector3Enu(-3.0, -3.0, -3.0))

    describe("*"):
      it("multiplies vector by scalar"):
        val v = Vector3Enu(2.0, 3.0, 4.0)

        assert(v * 2.0 === Vector3Enu(4.0, 6.0, 8.0))

      it("multiplies vector by zero"):
        val v = Vector3Enu(2.0, 3.0, 4.0)

        assert(v * 0.0 === Vector3Enu(0.0, 0.0, 0.0))

      it("multiplies vector by negative scalar"):
        val v = Vector3Enu(2.0, 3.0, 4.0)

        assert(v * -2.0 === Vector3Enu(-4.0, -6.0, -8.0))

    describe("/"):
      it("divides vector by scalar"):
        val v = Vector3Enu(4.0, 6.0, 8.0)

        assert(v / 2.0 === Vector3Enu(2.0, 3.0, 4.0))

      it("divides vector by negative scalar"):
        val v = Vector3Enu(4.0, 6.0, 8.0)

        assert(v / -2.0 === Vector3Enu(-2.0, -3.0, -4.0))

      it("divides vector by zero resulting in infinity"):
        val v = Vector3Enu(1.0, 2.0, 3.0)
        val result = v / 0.0

        assert(result.x.isInfinity)
        assert(result.y.isInfinity)
        assert(result.z.isInfinity)

    describe("distance"):
      it("calculates distance to same point"):
        val v1 = Vector3Enu(1.0, 2.0, 3.0)
        val v2 = Vector3Enu(1.0, 2.0, 3.0)

        assert(v1.distance(v2) === 0.0)

      it("calculates distance along single axis"):
        val v1 = Vector3Enu(0.0, 0.0, 0.0)
        val v2 = Vector3Enu(3.0, 0.0, 0.0)

        assert(v1.distance(v2) === 3.0)

      it("calculates distance with Pythagorean triple"):
        val v1 = Vector3Enu(0.0, 0.0, 0.0)
        val v2 = Vector3Enu(3.0, 4.0, 12.0)

        assert(v1.distance(v2) === 13.0)

      it("returns symmetric results"):
        val v1 = Vector3Enu(1.0, 2.0, 3.0)
        val v2 = Vector3Enu(4.0, 5.0, 6.0)

        assert(v1.distance(v2) === v2.distance(v1))
