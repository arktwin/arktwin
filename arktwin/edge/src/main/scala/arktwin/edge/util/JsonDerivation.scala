// SPDX-License-Identifier: Apache-2.0
// Copyright 2024-2025 TOYOTA MOTOR CORPORATION
package arktwin.edge.util

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter}
import sttp.tapir.Schema.SName
import sttp.tapir.SchemaType.{SCoproduct, SProduct, SProductField, SString}
import sttp.tapir.Validator.Enumeration
import sttp.tapir.generic.auto.SchemaDerivation
import sttp.tapir.{FieldName, Schema}

import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration.{Duration, FiniteDuration}

object JsonDerivation extends SchemaDerivation:
  private def shortNameFrom(sName: SName): String = sName.fullName.split('.').last

  // fix tapir coproduct schema to match jsoniter-scala codec without discriminator
  def fixCoproductSchemaWithoutDiscriminator[A](originalSchema: Schema[A]): Schema[A] =
    originalSchema.schemaType match
      case SCoproduct(childSchemas, _) =>
        var enumStrings = List[String]()
        val newSchemas = childSchemas.flatMap:
          case Schema(SProduct(List()), Some(sName), _, _, _, _, _, _, _, _, _) =>
            // name of string schema when there are no fields
            val shortName = shortNameFrom(sName)
            // hide Empty of sealed oneof genererated by ScalaPB
            if shortName != "Empty" then enumStrings = enumStrings :+ shortName
            None

          case childSchema @ Schema(SProduct(_), Some(sName), _, _, _, _, _, _, _, _, _) =>
            // wrapped schema with only one field that has the same name as original schema
            val shortName = shortNameFrom(sName)
            Some(
              Schema(
                SProduct[A](
                  List(SProductField(FieldName(shortName, shortName), childSchema, _ => None))
                )
              )
            )

          case childSchema =>
            Some(childSchema)

        val enumSchema =
          if enumStrings.nonEmpty
          then Some(Schema(SString()).validate(Enumeration(enumStrings, None, None)))
          else None

        originalSchema.copy(schemaType = SCoproduct(newSchemas ++ enumSchema, None)(_ => None))

      case _ =>
        originalSchema

  given schemaFiniteDuration: Schema[FiniteDuration] = Schema.string

  given codecFiniteDuration: JsonValueCodec[FiniteDuration] = new JsonValueCodec[FiniteDuration]:
    override val nullValue: FiniteDuration = null

    override def encodeValue(x: FiniteDuration, out: JsonWriter): Unit = out.writeVal(x.toString())

    override def decodeValue(in: JsonReader, default: FiniteDuration): FiniteDuration =
      val s = in.readString(null)
      try
        Duration(s) match
          case fd: FiniteDuration => fd
          case _: Infinite        => in.readNullOrTokenError(default, '"')
      catch
        case _: NumberFormatException =>
          in.readNullOrTokenError(default, '"')
