package net.propoint.fun.definition

import java.util.UUID

import org.joda.time.{DateTime, DateTimeZone}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}


object PurchaseOrderDefinition {
  import cats.implicits._
  
  case class Person(name: String)

  case class IdentifierMapRequest(
                                   correlationId: UUID,
                                   eventSource: String,
                                   purchaseOrder: PurchaseOrder
                                 )
  case class Item(
                          epmSku: Long,
                          quantityOrdered: Int,
                          unitCost: Double
                        )
  
  case class PurchaseOrder(
                            purchaseOrderNumber: Long,
                            salesEventId: Option[Long],
                            startShipDate: Option[DateTime],
                            endShipDate: Option[DateTime],
                            departmentId: Option[Int],
                            items: List[Item]
                          )

  object PurchaseOrder {
    implicit val encodeIdentifiersPayload: Encoder[Item] = deriveEncoder[Item]
    implicit val encodePurchaseOrderPayload: Encoder[PurchaseOrder] = deriveEncoder[PurchaseOrder]
    implicit val dateTimeEncoder: Encoder[DateTime] = Encoder[String].contramap(d => d.toString)

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss Z").withZone(DateTimeZone.forID("America/Los_Angeles"))
    implicit val dateTimeDecoder: Decoder[DateTime] = Decoder.decodeString.emap(str => Either.catchNonFatal(DateTime.parse(str, dateTimeFormatter)).leftMap(_.getMessage))
    implicit val decodeIdentifiersPayload: Decoder[Item] = deriveDecoder[Item]
    implicit val decodePurchaseOrderPayload: Decoder[PurchaseOrder] = deriveDecoder[PurchaseOrder]
  }
}
