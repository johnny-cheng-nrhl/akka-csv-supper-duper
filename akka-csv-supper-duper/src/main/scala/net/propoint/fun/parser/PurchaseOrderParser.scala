package net.propoint.fun.parser

import net.propoint.fun.definition.PurchaseOrderDefinition.{Person, PurchaseOrder}
import io.circe
import io.circe.parser._
import io.circe.syntax._

object PurchaseOrderParser {
  def decodePurchaseOrder(purchaseOrderJson: String): Either[circe.Error, PurchaseOrder] = {
    decode[PurchaseOrder](purchaseOrderJson)
  }

  def encodePurchaseOrder(purchaseOrder: PurchaseOrder): String =
    purchaseOrder.asJson.toString()
}
