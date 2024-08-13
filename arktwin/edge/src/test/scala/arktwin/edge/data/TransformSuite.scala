// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.Timestamp
import arktwin.edge.data.EulerAnglesConfig.Order.*
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TransformSuite extends AnyFunSuite with Matchers:
  given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.001)

  given vector3Equality: Equality[Vector3] =
    case (a: Vector3, b: Vector3) =>
      a.toDoubles.zip(b.toDoubles).forall(doubleEquality.areEqual.tupled)
    case _ =>
      false

  given rotationEquality: Equality[Rotation] =
    case (a: EulerAngles, b: EulerAngles) =>
      val f = (e: EulerAngles) => Seq(e.x, e.y, e.z)
      f(a).zip(f(b)).forall(doubleEquality.areEqual.tupled)
    case (a: Quaternion, b: Quaternion) =>
      val f = (e: Quaternion) => Seq(e.x, e.y, e.z, e.w)
      f(a).zip(f(b)).forall(doubleEquality.areEqual.tupled)
    case _ =>
      false

  given transformEquality: Equality[Transform] =
    case (a: Transform, b: Transform) =>
      a.parentAgentId == b.parentAgentId &&
      vector3Equality.areEqual(a.globalScale, b.globalScale) &&
      rotationEquality.areEqual(a.localRotation, b.localRotation) &&
      vector3Equality.areEqual(a.localTranslation, b.localTranslation)
    // && vector3Equality.areEqual(a.localTranslationSpeed, b.localTranslationSpeed)
    case _ =>
      false

  // TODO test rotation matrix
  // TODO test cases referring scipy.spatial.transform?
  test("Rotation"):
    for
      vector3Setting <-
        import Vector3Config.Direction.*
        import Vector3Config.LengthUnit.*
        import Vector3Config.TimeUnit.*
        // TODO all patterns (1*6*8)
        Seq(Vector3Config(Meter, Second, East, North, Up), Vector3Config(Meter, Second, North, East, Down))
      order <- Seq(XYZ, XZY, YXZ, YZX, ZXY, ZYX)
    do
      import EulerAnglesConfig.AngleUnit.*
      val setting = CoordinateConfig(vector3Setting, EulerAnglesConfig(Degree, order))
      val t = Transform(
        None,
        Vector3(1, 1, 1),
        EulerAngles(10, 20, 30),
        Vector3(1, 2, 3),
        None
      )
      Transform.fromEnu(t.toEnu(Timestamp(0, 0), None, setting), setting) shouldEqual t

    for
      transformSetting <-
        import Vector3Config.Direction.*
        import Vector3Config.LengthUnit.*
        import Vector3Config.TimeUnit.*
        // TODO all patterns (1*6*8)
        Seq(Vector3Config(Meter, Second, East, North, Up), Vector3Config(Meter, Second, North, East, Down))
      order <- Seq(XYZ, XZY, YXZ, YZX, ZXY, ZYX)
    do
      import EulerAnglesConfig.AngleUnit.*
      val setting = CoordinateConfig(transformSetting, EulerAnglesConfig(Radian, order))
      val t = Transform(
        None,
        Vector3(1, 1, 1),
        EulerAngles(0.1, 0.2, 0.3),
        Vector3(1, 2, 3),
        None
      )
      Transform.fromEnu(t.toEnu(Timestamp(0, 0), None, setting), setting) shouldEqual t

  test("permutation"):
    import Vector3Config.Direction.*
    import Vector3Config.LengthUnit.*
    import Vector3Config.TimeUnit.*
    val sourceSetting = CoordinateConfig(Vector3Config(Meter, Second, East, North, Up), QuaternionConfig)
    val targetSetting = CoordinateConfig(Vector3Config(Meter, Second, North, East, Down), QuaternionConfig)
    val t = Transform(
      None,
      Vector3(1, 1, 1),
      Quaternion(0, 0, 0, 1),
      Vector3(1, 2, 3),
      None
    )
    Transform
      .fromEnu(t.toEnu(Timestamp(0, 0), None, sourceSetting), targetSetting)
      .localTranslation shouldEqual Vector3(2, 1, -3)
