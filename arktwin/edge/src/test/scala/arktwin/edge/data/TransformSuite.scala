// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.data

import arktwin.common.data.VirtualTimestamp
import arktwin.edge.configs.AxisConfig.Direction.*
import arktwin.edge.configs.CoordinateConfig.LengthUnit.*
import arktwin.edge.configs.CoordinateConfig.SpeedUnit.*
import arktwin.edge.configs.EulerAnglesConfig.AngleUnit.*
import arktwin.edge.configs.EulerAnglesConfig.RotationMode.*
import arktwin.edge.configs.EulerAnglesConfig.RotationOrder.*
import arktwin.edge.configs.{AxisConfig, CoordinateConfig, EulerAnglesConfig, QuaternionConfig}
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class TransformSuite extends AnyFunSuite with Matchers:
  given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.05)

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

  given optionVector3Equality: Equality[Option[Vector3]] =
    case (Some(a), Some(b)) =>
      vector3Equality.areEqual(a, b)
    case (None, None) =>
      true
    case _ =>
      false

  given Equality[Transform] =
    case (a: Transform, b: Transform) =>
      a.parentAgentId == b.parentAgentId &&
      vector3Equality.areEqual(a.globalScale, b.globalScale) &&
      rotationEquality.areEqual(a.localRotation, b.localRotation) &&
      vector3Equality.areEqual(a.localTranslation, b.localTranslation) &&
      optionVector3Equality.areEqual(a.localTranslationSpeed, b.localTranslationSpeed)
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
      mode <- Seq(Extrinsic, Intrinsic)
      order <- Seq(XYZ, YZX, ZYX)
      lengthUnit <- Seq(Meter, Kilometer)
      speedUnit <- Seq(MeterPerSecond, MillimeterPerMinute, KilometerPerHour)
    do
      val config = CoordinateConfig(
        axisConfig,
        Vector3(1.1, 2.2, 3.3),
        EulerAnglesConfig(angleUnit, mode, order),
        lengthUnit,
        speedUnit
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
        s"axisConfig: $axisConfig, angleUnit: $angleUnit, order: $order, lengthUnit: $lengthUnit, speedUnit: $speedUnit"
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
      speedUnit <- Seq(MeterPerSecond, MillimeterPerMinute, KilometerPerHour)
    do
      val config = CoordinateConfig(
        axisConfig,
        Vector3(1.1, 2.2, 3.3),
        QuaternionConfig,
        lengthUnit,
        speedUnit
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
        s"axisConfig: $axisConfig, angleUnit: $angleUnit, order: $order, lengthUnit: $lengthUnit, speedUnit: $speedUnit"
      ) {
        Transform(t.normalize(VirtualTimestamp(0, 0), None, config), config) shouldEqual t
      }

  test("axis permutation"):
    val sourceSetting =
      CoordinateConfig(
        AxisConfig(East, North, Up),
        Vector3(0, 0, 0),
        QuaternionConfig,
        Meter,
        MeterPerSecond
      )
    val targetSetting =
      CoordinateConfig(
        AxisConfig(North, East, Down),
        Vector3(0, 0, 0),
        QuaternionConfig,
        Meter,
        MeterPerSecond
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

  test("coordinate transformation between Unreal Engine and Unity - case 1"):
    val configUE = CoordinateConfig(
      AxisConfig(East, South, Up),
      Vector3(400, -500, 200),
      EulerAnglesConfig(Degree, Extrinsic, XYZ),
      Centimeter,
      MeterPerSecond
    )
    val configUnity = CoordinateConfig(
      AxisConfig(South, Up, East),
      Vector3(3, -7, -9),
      QuaternionConfig,
      Meter,
      MeterPerSecond
    )
    val transformUE = Transform(
      None,
      Vector3(1, 1, 1),
      EulerAngles(60, 30, -60),
      Vector3(1700, 1700, -1300),
      Some(Vector3(-1, -2, 3)),
      None
    )
    val transformUnity = Transform(
      None,
      Vector3(1, 1, 1),
      /*
      <local rotation transformation steps>
      1.exchange axis -> 2.calculate Quaternion

      1.exchange axis
      AxisConfig(UE) = (East, South, Up)
      AxisConfig(Unity) = (South, Up, East)
      EulerAngles(UE) (x, y, z) = (60, 30, -60)
      EulerAngles(Unity) (x, y, z) = (30, -60, 60)
      RotationOrder(UE) = XYZ
      RotationOrder(Unity) = ZXY

      2.calculate Quaternion
      Quaternion(Unity) (RotationOrder : ZXY)
      q_x = cos(30/2)sin(-60/2)sin(60/2) + sin(30/2)cos(-60/2)cos(60/2) = -0.047367
      q_y = -sin(30/2)cos(-60/2)sin(60/2) + cos(30/2)sin(-60/2)cos(60/2) = -0.530330
      q_z = cos(30/2)cos(-60/2)sin(60/2) - sin(30/2)sin(-60/2)cos(60/2) = 0.530330
      q_w = sin(30/2)sin(-60/2)sin(60/2) + cos(30/2)cos(-60/2)cos(60/2) = 0.659739
       */
      Quaternion(-0.047367, -0.530330, 0.530330, 0.659739),
      /*
      <local position transformation steps>
      1.calculate vector on UE-axis -> 2.exchange axis & unit -> 3.calculate vector on Unity-axis

      1.calculate vector on UE-axis
      Vector(UE origin -> global origin) = (400, -500, 200)
      Vector(UE origin -> position) = (1700, 1700, -1300)
      Vector(global origin -> position) = (1700, 1700, -1300) - (400, -500, 200) = (1300, 2200, -1500)

      2.exchange axis & unit
      AxisConfig(UE) = (East, South, Up) [cm]
      AxisConfig(Unity) = (South, Up, East) [m]
      "on Unity-axis"
      Vector(global origin -> position) = (22, -15, 13)

      3.calculate vector on Unity-axis
      Vector(Unity origin -> global origin) = (3, -7, -9)
      Vector(Unity origin -> position)
      = Vector(Unity origin -> global origin) + Vector(global origin -> position)
      = (3, -7, -9) + (22, -15, 13)
      = (25, -22, 4)
       */
      Vector3(25, -22, 4),
      /*
      <local speed transformation step>
      1.exchange axis
      AxisConfig(UE) = (East, South, Up)
      AxisConfig(Unity) = (South, Up, East)
      Vx(Unity) = Vy(UE) = -2
      Vy(Unity) = Vz(UE) = 3
      Vz(Unity) = Vx(UE) = -1
       */
      Some(Vector3(-2, 3, -1)),
      None
    )
    Transform(
      transformUE.normalize(VirtualTimestamp(0, 0), None, configUE),
      configUnity
    ) shouldEqual transformUnity
    Transform(
      transformUnity.normalize(VirtualTimestamp(0, 0), None, configUnity),
      configUE
    ) shouldEqual transformUE

  test("coordinate transformation between Unreal Engine and Unity - case 2"):
    val configUnity = CoordinateConfig(
      AxisConfig(East, Up, North),
      Vector3(-13.4, -25.2, 9.3),
      QuaternionConfig,
      Meter,
      MeterPerSecond
    )
    val configUE = CoordinateConfig(
      AxisConfig(South, West, Up),
      Vector3(1230, -460, 950),
      EulerAnglesConfig(Degree, Extrinsic, XYZ),
      Centimeter,
      MeterPerSecond
    )
    val transformUnity = Transform(
      None,
      Vector3(1, 1, 1),
      Quaternion(-0.2619, 0.6428, 0.6127, 0.3780),
      Vector3(31.2, 12.0, 14.7),
      Some(Vector3(-12.1, 0.7, 4.8)),
      None
    )
    val transformUE = Transform(
      None,
      Vector3(-1, -1, 1),
      /*
      <local rotation transformation steps>
      1.exchange axis -> 2.calculate EulerAngle

      1.exchange axis
      AxisConfig(Unity) = (East, Up, North)
      AxisConfig(UE) = (South, West, Up)
      q_x(UE) = -q_z(Unity) = -0.6127
      q_y(UE) = -q_x(Unity) = 0.2619
      q_z(UE) = q_y(Unity) = 0.6428
      q_w(UE) = q_w(Unity) = 0.3780

      2.calculate EulerAngle
      EulerAngle(UE) (RotationOrder : XYZ)
      θ_x = atan2(2*(q_x*q_w + q_y*q_z), 2*q_w*q_w + 2*q_z*q_z - 1) = -48.4411957
      θ_y = asin(2*q_y*q_w - 2*q_x*q_z) = 80.29321855
      θ_z = atan2(2*(q_x*q_y + q_z*q_w), 2*q_w*q_w + 2*q_x*q_x - 1) = 77.50479187
       */
      EulerAngles(-48.4411957, 80.29321855, 77.50479187),
      /*
      <local position transformation steps>
      1.calculate vector on Unity-axis -> 2.exchange axis & unit -> 3.calculate vector on UE-axis

      1.calculate vector on Unity-axis
      Vector(Unity origin -> global origin) = (-13.4, -25.2, 9.3)
      Vector(Unity origin -> position) = (31.2, 12.0, 14.7)
      Vector(global origin -> position) = (31.2, 12.0, 14.7) - (-13.4, -25.2, 9.3) = (44.6, 37.2, 5.4)

      2.exchange axis & unit
      AxisConfig(Unity) = (East, Up, North) [m]
      AxisConfig(UE) = (South, West, Up) [cm]
      "on UE-axis"
      Vector(global origin -> position) = (-540, -4460, 3720)

      3.calculate vector on UE-axis
      Vector(UE origin -> global origin) = (1230, -460, 950)
      Vector(UE origin -> position)
      = Vector(UE origin -> global origin) + Vector(global origin -> position)
      = (1230, -460, 950) + (-540, -4460, 3720)
      = (690, -4920, 4670)
       */
      Vector3(690, -4920, 4670),
      /*
      <local speed transformation step>
      1.exchange axis
      AxisConfig(Unity) = (East, Up, North)
      AxisConfig(UE) = (South, West, Up)
      Vx(UE) = -Vz(Unity) = -4.8
      Vy(UE) = -Vx(Unity) = 12.1
      Vz(UE) = Vy(Unity) = 0.7
       */
      Some(Vector3(-4.8, 12.1, 0.7)),
      None
    )
    Transform(
      transformUnity.normalize(VirtualTimestamp(0, 0), None, configUnity),
      configUE
    ) shouldEqual transformUE
    Transform(
      transformUE.normalize(VirtualTimestamp(0, 0), None, configUE),
      configUnity
    ) shouldEqual transformUnity

  test("coordinate transformation between Unreal Engine and Unity - case 3"):
    val configUE = CoordinateConfig(
      AxisConfig(South, West, Up),
      Vector3(-3260, -4225, 12),
      EulerAnglesConfig(Degree, Extrinsic, XYZ),
      Centimeter,
      MeterPerSecond
    )
    val configUnity = CoordinateConfig(
      AxisConfig(North, Up, West),
      Vector3(21.4, -0.2, -2.9),
      QuaternionConfig,
      Meter,
      MeterPerSecond
    )
    val transformUE = Transform(
      None,
      Vector3(1, 1, 1),
      EulerAngles(-117.2, -25.5, 50),
      Vector3(-929, -2193, 648),
      Some(Vector3(6.7, 3.1, -1.6)),
      None
    )
    val transformUnity = Transform(
      None,
      Vector3(-1, 1, 1),
      /*
      <local rotation transformation steps>
      1.exchange axis -> 2.calculate Quaternion

      1.exchange axis
      AxisConfig(UE) = (South, West, Up)
      AxisConfig(Unity) = (North, Up, West)
      EulerAngles(UE) (x, y, z) = (-117.2, -25.5, 50)
      EulerAngles(Unity) (x, y, z) = (117.2, 50, -25.5)
      RotationOrder(UE) = XYZ
      RotationOrder(Unity) = XZY

      2.calculate Quaternion
      Quaternion(Unity) (RotationOrder : XZY)
      q_x = cos(117.2/2)sin(50/2)sin(-25.5/2) + sin(117.2/2)cos(50/2)cos(-25.5/2) = 0.705910085
      q_y = sin(117.2/2)cos(50/2)sin(-25.5/2) + cos(117.2/2)sin(50/2)cos(-25.5/2) = 0.044031792
      q_z = cos(117.2/2)cos(50/2)sin(-25.5/2) - sin(117.2/2)sin(50/2)cos(-25.5/2) = -0.456043729
      q_w = -sin(117.2/2)sin(50/2)sin(-25.5/2) + cos(117.2/2)cos(50/2)cos(-25.5/2) = 0.540163188
       */
      Quaternion(0.705910085, 0.044031792, -0.456043729, 0.540163188),
      /*
      <local position transformation steps>
      1.calculate vector on UE-axis -> 2.exchange axis & unit -> 3.calculate vector on Unity-axis

      1.calculate vector on UE-axis
      Vector(UE origin -> global origin) = (-3260, -4225, 12)
      Vector(UE origin -> position) = (-929, -2193, 648)
      Vector(global origin -> position) = (-929, -2193, 648) - (-3260, -4225, 12) = (2331, 2032, 636)

      2.exchange axis & unit
      AxisConfig(UE) = (South, West, Up) [cm]
      AxisConfig(Unity) = (North, Up, West) [m]
      "on Unity-axis"
      Vector(global origin -> position) = (-23.31, 6.36, 20.32)

      3.calculate vector on Unity-axis
      Vector(Unity origin -> global origin) = (21.4, -0.2, -2.9)
      Vector(Unity origin -> position)
      = Vector(Unity origin -> global origin) + Vector(global origin -> position)
      = (-23.31, 6.36, 20.32) + (21.4, -0.2, -2.9)
      = (-1.91, 6.16, 17.42)
       */
      Vector3(-1.91, 6.16, 17.42),
      /*
      <local speed transformation step>
      1.exchange axis
      AxisConfig(UE) = (South, West, Up)
      AxisConfig(Unity) = (North, Up, West)
      Vx(Unity) = -Vx(UE) = -6.7
      Vy(Unity) = Vz(UE) = -1.6
      Vz(Unity) = Vy(UE) = 3.1
       */
      Some(Vector3(-6.7, -1.6, 3.1)),
      None
    )
    Transform(
      transformUE.normalize(VirtualTimestamp(0, 0), None, configUE),
      configUnity
    ) shouldEqual transformUnity
    Transform(
      transformUnity.normalize(VirtualTimestamp(0, 0), None, configUnity),
      configUE
    ) shouldEqual transformUE

  test("coordinate transformation between Unreal Engine and Unity - case 4"):
    val configUnity = CoordinateConfig(
      AxisConfig(South, Up, East),
      Vector3(9.5, 0.4, -7.3),
      QuaternionConfig,
      Meter,
      MeterPerSecond
    )
    val configUE = CoordinateConfig(
      AxisConfig(North, East, Up),
      Vector3(4.7, 3.2, 0.1),
      EulerAnglesConfig(Degree, Extrinsic, XYZ),
      Meter,
      MeterPerSecond
    )
    val transformUnity = Transform(
      None,
      Vector3(1, 1, 1),
      Quaternion(-0.918762872, 0.267438819, 0.258014898, 0.133340076),
      Vector3(-123.8, 5.7, 97.6),
      Some(Vector3(0.5, 0.05, -1.2)),
      None
    )
    val transformUE = Transform(
      None,
      Vector3(-1, 1, 1),
      /*
      <local rotation transformation steps>
      1.exchange axis -> 2.calculate EulerAngle

      1.exchange axis
      AxisConfig(Unity) = (South, Up, East)
      AxisConfig(UE) = (North, East, Up)
      q_x(UE) = -q_x(Unity) = 0.918762872
      q_y(UE) = q_z(Unity) = 0.258014898
      q_z(UE) = q_y(Unity) = 0.267438819
      q_w(UE) = q_w(Unity) = 0.133340076

      2.calculate EulerAngle
      EulerAngle(UE) (RotationOrder : XYZ)
      θ_x = atan2(2*(q_x*q_w + q_y*q_z), 2*q_w*q_w + 2*q_z*q_z - 1) = 155
      θ_y = asin(2*q_y*q_w - 2*q_x*q_z) = -25
      θ_z = atan2(2*(q_x*q_y + q_z*q_w), 2*q_w*q_w + 2*q_x*q_x - 1) = 37
       */
      EulerAngles(155, -25, 37),
      /*
      <local position transformation steps>
      1.calculate vector on Unity-axis -> 2.exchange axis & unit -> 3.calculate vector on UE-axis

      1.calculate vector on Unity-axis
      Vector(Unity origin -> global origin) = (9.5, 0.4, -7.3)
      Vector(Unity origin -> position) = (-123.8, 5.7, 97.6)
      Vector(global origin -> position) = (-123.8, 5.7, 97.6) - (9.5, 0.4, -7.3) = (-133.3, 5.3, 104.9)

      2.exchange axis & unit
      AxisConfig(Unity) = (South, Up, East) [m]
      AxisConfig(UE) = (North, East, Up) [m]
      "on UE-axis"
      Vector(global origin -> position) = (133.3, 104.9, 5.3)

      3.calculate vector on UE-axis
      Vector(UE origin -> global origin) = (4.7, 3.2, 0.1)
      Vector(UE origin -> position)
      = Vector(UE origin -> global origin) + Vector(global origin -> position)
      = (4.7, 3.2, 0.1) + (133.3, 104.9, 5.3)
      = (138.0, 108.1, 5.4)
       */
      Vector3(138.0, 108.1, 5.4),
      /*
      <local speed transformation step>
      1.exchange axis
      AxisConfig(Unity) = (South, Up, East)
      AxisConfig(UE) = (North, East, Up)
      Vx(UE) = -Vx(Unity) = -0.5
      Vy(UE) = Vz(Unity) = -1.2
      Vz(UE) = Vy(Unity) = 0.05
       */
      Some(Vector3(-0.5, -1.2, 0.05)),
      None
    )
    Transform(
      transformUnity.normalize(VirtualTimestamp(0, 0), None, configUnity),
      configUE
    ) shouldEqual transformUE
    Transform(
      transformUE.normalize(VirtualTimestamp(0, 0), None, configUE),
      configUnity
    ) shouldEqual transformUnity
