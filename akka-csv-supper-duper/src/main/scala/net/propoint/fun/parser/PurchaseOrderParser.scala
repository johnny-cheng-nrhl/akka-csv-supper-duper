package net.propoint.fun.parser

import net.propoint.fun.definition.PurchaseOrderDefinition.{Person, PurchaseOrder}
import io.circe
import io.circe.parser._
import io.circe.syntax._

object PurchaseOrderParser {
//  def decodePerson(personJson: String): Either[circe.Error, Person] =
//    decode[Person](personJson)
//
//  def encodePerson(person: Person): String =
//    person.asJson.toString()
  
  def decodePurchaseOrder(purchaseOrderJson: String): Either[circe.Error, PurchaseOrder] = {
    // println(purchaseOrderJson)
    decode[PurchaseOrder](purchaseOrderJson)
  }

  def encodePurchaseOrder(purchaseOrder: PurchaseOrder): String =
    purchaseOrder.asJson.toString()
}
