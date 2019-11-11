package net.propoint.fun

import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.{ActorMaterializer, Graph, IOResult, SinkShape, Supervision}
import akka.stream.scaladsl.{FileIO, Flow, JsonFraming, Keep, Sink, Source}
import akka.util.ByteString
import io.circe
import net.propoint.fun.definition.PurchaseOrderDefinition.{Item, Person, PurchaseOrder}
import net.propoint.fun.parser.PurchaseOrderParser

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object JsonConsumer extends App {
  
  // implicit actor system
  implicit val system = ActorSystem("Sys")

  // implicit actor materializer
  implicit val materializer = ActorMaterializer()

  val path = Paths.get("src/main/resources/po_backfill_4.json")
  println(path)

  private val jsonFileSource = FileIO.fromPath(path)
  type PurchaseOrderFlowElement = Either[circe.Error, PurchaseOrder]
  
  def dedupedPurchaseOrder(purchaseOrderFlowElement: PurchaseOrderFlowElement): PurchaseOrderFlowElement = {
    def consolidateDuplicateSkusQuantities(
                                            orderItems: List[Item]
                                          ): List[Item] = {
      val quantitiesByEpmSku: Map[Long, Long] = orderItems.groupBy(_.epmSku).mapValues(_.map(_.quantityOrdered).sum)

      val dedupedIterable = for {
        (_, items) <- orderItems.groupBy(_.epmSku)
        item <- items.headOption
        summedQuantity <- quantitiesByEpmSku.get(item.epmSku)
      } yield item.copy(quantityOrdered = summedQuantity.toInt)

      dedupedIterable.toList
    }
    
    for {
      purchaseOrder <- purchaseOrderFlowElement
    } yield purchaseOrder.copy(items = consolidateDuplicateSkusQuantities(purchaseOrder.items))
  }
  
  val selectPurchaseOrderFlow: Flow[ByteString, ByteString, NotUsed] =
    JsonReader.select("$.purchaseOrder")

  val convertByteToStringFlow: Flow[ByteString, String, NotUsed] =
    Flow[ByteString].map(_.utf8String)

  val parseStringToPurchaseOrderFlow: Flow[String, PurchaseOrderFlowElement , NotUsed] =
    Flow[String].map { s =>
      PurchaseOrderParser.decodePurchaseOrder(s)
    }
  
  val dedupedPurchaseOrderFlow: Flow[PurchaseOrderFlowElement, PurchaseOrderFlowElement, NotUsed] =
    Flow[PurchaseOrderFlowElement].map(dedupedPurchaseOrder(_))

  val results =
    jsonFileSource
      .via(selectPurchaseOrderFlow)
      .via(convertByteToStringFlow)
      .via(parseStringToPurchaseOrderFlow)
      .via(dedupedPurchaseOrderFlow)
      .toMat(Sink.fold(Seq.empty[String]) {
        case (acc, entry) => acc ++ Seq(entry.toString())
      })(Keep.right)
      .withAttributes(supervisionStrategy(Supervision.resumingDecider))
      .run()
  
  private val reply = Await.result(results, 10 seconds)
  println(s"Received $reply")
  Await.ready(system.terminate(), 10 seconds)
}
