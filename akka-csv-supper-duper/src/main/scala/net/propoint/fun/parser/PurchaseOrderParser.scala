package net.propoint.fun.parser

import net.propoint.fun.definition.PurchaseOrderDefinition._
import io.circe
import io.circe.parser._
import io.circe.syntax._

object PurchaseOrderParser {
  def decodePurchaseOrder(personJson: String): Either[circe.Error, Person] =
    decode[Person](personJson)
  
  def encodePurchaseOrder(person: Person): String =
    person.asJson.toString()
}
