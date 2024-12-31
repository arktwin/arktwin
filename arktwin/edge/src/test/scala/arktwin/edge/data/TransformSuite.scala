// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.VirtualTimestamp
import arktwin.edge.configs.AxisConfig.Direction.*
import arktwin.edge.configs.CoordinateConfig.LengthUnit.*
import arktwin.edge.configs.CoordinateConfig.TimeUnit.*
import arktwin.edge.configs.EulerAnglesConfig.AngleUnit.*
import arktwin.edge.configs.EulerAnglesConfig.RotationOrder.*
import arktwin.edge.configs.{AxisConfig, CoordinateConfig, EulerAnglesConfig, QuaternionConfig}
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TransformSuite extends AnyFunSuite with Matchers:
  given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.001)

  given vector3Equality: Equality[Vector3] =
    case (a: Vector3, b: Vector3) =>
      Seq(a.x, a.y, a.z).zip(Seq(b.x, b.y, b.z)).forall(doubleEquality.areEqual.tupled)
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

  given translationSpeedEquality: Equality[Option[Vector3]] =
    case (Some(a), Some(b)) =>
      vector3Equality.areEqual(a, b)
    case (None, None) =>
      true
    case _ =>
      false

  given transformEquality: Equality[Transform] =
    case (a: Transform, b: Transform) =>
      a.parentAgentId == b.parentAgentId &&
      vector3Equality.areEqual(a.globalScale, b.globalScale) &&
      rotationEquality.areEqual(a.localRotation, b.localRotation) &&
      vector3Equality.areEqual(a.localTranslation, b.localTranslation) &&
      translationSpeedEquality.areEqual(a.localTranslationSpeed, b.localTranslationSpeed)
    case _ =>
      false

  test("denormalizing value equals to the original value with the same config"):
    for
      axisConfig <-
        Seq(
          AxisConfig(East, North, Up),
          AxisConfig(North, East, Down),
          AxisConfig(South, Up, West)
        )
      angleUnit <- Seq(Degree, Radian)
      order <- Seq(XYZ, YZX, ZYX)
      lengthUnit <- Seq(Meter, Kilometer)
      timeUnit <- Seq(Second, Minute)
    do
      val config = CoordinateConfig(
        Vector3(1.1, 2.2, 3.3),
        axisConfig,
        EulerAnglesConfig(angleUnit, order),
        lengthUnit,
        timeUnit
      )
      val t = Transform(
        None,
        Vector3(1, 1, 1),
        EulerAngles(0.1, 0.2, 0.3),
        Vector3(1.2, 3.4, 5.6),
        Some(Vector3(9.8, 7.6, 5.4)),
        None
      )
      withClue(
        s"axisConfig: $axisConfig, angleUnit: $angleUnit, order: $order, lengthUnit: $lengthUnit, timeUnit: $timeUnit"
      ) {
        Transform(t.normalize(VirtualTimestamp(0, 0), None, config), config) shouldEqual t
      }

    for
      axisConfig <-
        Seq(
          AxisConfig(East, North, Up),
          AxisConfig(North, East, Down),
          AxisConfig(South, Up, West)
        )
      angleUnit <- Seq(Degree, Radian)
      order <- Seq(XYZ, YZX, ZYX)
      lengthUnit <- Seq(Meter, Kilometer)
      timeUnit <- Seq(Second, Minute)
    do
      val config = CoordinateConfig(
        Vector3(1.1, 2.2, 3.3),
        axisConfig,
        QuaternionConfig,
        lengthUnit,
        timeUnit
      )
      val t = Transform(
        None,
        Vector3(1, 1, 1),
        Quaternion(0.1, 0.2, 0.3, 0.4),
        Vector3(1.2, 3.4, 5.6),
        Some(Vector3(9.8, 7.6, 5.4)),
        None
      )
      withClue(
        s"axisConfig: $axisConfig, angleUnit: $angleUnit, order: $order, lengthUnit: $lengthUnit, timeUnit: $timeUnit"
      ) {
        Transform(t.normalize(VirtualTimestamp(0, 0), None, config), config) shouldEqual t
      }

  test("axis permutation"):
    val sourceSetting =
      CoordinateConfig(
        Vector3(0, 0, 0),
        AxisConfig(East, North, Up),
        QuaternionConfig,
        Meter,
        Second
      )
    val targetSetting =
      CoordinateConfig(
        Vector3(0, 0, 0),
        AxisConfig(North, East, Down),
        QuaternionConfig,
        Meter,
        Second
      )
    val t = Transform(
      None,
      Vector3(1, 1, 1),
      Quaternion(0, 0, 0, 1),
      Vector3(1, 2, 3),
      None,
      None
    )
    Transform(
      t.normalize(VirtualTimestamp(0, 0), None, sourceSetting),
      targetSetting
    ).localTranslation shouldEqual Vector3(2, 1, -3)

  // TODO test cases referring scipy.spatial.transform?
