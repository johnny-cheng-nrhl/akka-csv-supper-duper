package net.propoint.fun.definition

import java.util.UUID

import org.joda.time.DateTime
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import cats.implicits._

object PurchaseOrderDefinition {
  case class IdentifierMapRequest(
                                   correlationId: UUID,
                                   inboundOrderId: Long,
                                   eventSource: String,
                                   purchaseOrder: PurchaseOrder
                                 )

  case class PurchaseOrder(
                            purchaseOrderNumber: Long,
                            salesEventId: Long,
                            startShipDate: Option[DateTime],
                            endShipDate: Option[DateTime],
                            divisionId: Option[Int],
                            departmentId: Option[Int],
                            items: List[Identifiers]
                          )

  case class Identifiers(
                          epmSku: Long,
                          quantityOrdered: Int,
                          unitCost: Double
                        )

  case class Person(name: String)
  
  object Person {
    implicit val encodePersonPayload: Encoder[Person] = deriveEncoder[Person]
    
    implicit val decodePersonPayload: Decoder[Person] = deriveDecoder[Person]
  }
}
