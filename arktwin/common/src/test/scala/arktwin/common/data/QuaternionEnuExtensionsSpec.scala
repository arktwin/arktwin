// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.common.data

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import QuaternionEnuExtensions.*

class QuaternionEnuExtensionsSpec extends AnyFunSpec with Matchers:
  given Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-6)
  given Equality[QuaternionEnu] =
    case (a: QuaternionEnu, b: QuaternionEnu) =>
      Seq(a.x, a.y, a.z, a.w)
        .zip(Seq(b.x, b.y, b.z, b.w))
        .forall(summon[Equality[Double]].areEqual.tupled)
    case _ =>
      false

  describe("QuaternionEnu"):
    describe("*"):
      it("multiplies with identity quaternion"):
        val identity = QuaternionEnu(0.0, 0.0, 0.0, 1.0)
        val q = QuaternionEnu(0.1, 0.2, 0.3, 0.927362)

        assert(identity * q === q)
        assert(q * identity === q)

      it("multiplies with itself to yield double rotation"):
        val q = QuaternionEnu(0.0, 0.0, 0.707107, 0.707107) // 90 degree rotation around Z

        assert(q * q === QuaternionEnu(0.0, 0.0, 1.0, 0.0)) // 180 degree rotation around Z

      it("performs non-commutative multiplication"):
        val q1 = QuaternionEnu(0.707107, 0.0, 0.0, 0.707107) // 90 degree rotation around X
        val q2 = QuaternionEnu(0.0, 0.707107, 0.0, 0.707107) // 90 degree rotation around Y

        assert(q1 * q2 !== q2 * q1)

      it("multiplies with conjugate quaternion to yield identity quaternion"):
        val q = QuaternionEnu(0.1, 0.2, 0.3, 0.927362)
        val conjugate = QuaternionEnu(-0.1, -0.2, -0.3, 0.927362)

        assert(q * conjugate === QuaternionEnu(0.0, 0.0, 0.0, 1.0))
