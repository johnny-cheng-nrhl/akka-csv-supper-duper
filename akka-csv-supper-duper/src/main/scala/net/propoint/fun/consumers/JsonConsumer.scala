package net.propoint.fun.consumers

import java.nio.file.Paths

import cats.implicits._
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.{ActorMaterializer, Supervision}
import akka.stream.scaladsl.{FileIO, Flow, JsonFraming, Sink}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import io.circe
import net.propoint.fun.config.AppConfig
import net.propoint.fun.daos.{InboundOrderItemDao, InventoryDao}
import net.propoint.fun.definition.PurchaseOrderDefinition.{Item, PurchaseOrder}
import net.propoint.fun.parser.PurchaseOrderParser
import cats.effect.IO
import net.propoint.fun.models.{InboundOrderItemChange, InventoryChange}

import scala.concurrent.Future

class JsonConsumer(appConfig: AppConfig, inventoryDao: InventoryDao[IO], inboundOrderItemDao: InboundOrderItemDao[IO]) extends FlowConsumer with LazyLogging {

  // TODO configure the ExecutionContext
  import scala.concurrent.ExecutionContext.Implicits.global
  
  override implicit val system: ActorSystem = ActorSystem("json-consumer")
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  
  private val purchaseOrderParallelism = 2

  val path = Paths.get("src/main/resources/po_backfill_8.json")
  println(path)

  private val jsonFileSource = FileIO.fromPath(path)
  type PurchaseOrderFlowElement = Either[circe.Error, PurchaseOrder]
  
  
  def dedupedPurchaseOrder(purchaseOrder: PurchaseOrder): PurchaseOrder = {
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
    val dedupedItems = consolidateDuplicateSkusQuantities(purchaseOrder.items)
    logger.info(s"Finished deduping PurchaseOrder PO#: ${purchaseOrder.purchaseOrderNumber} items count ${dedupedItems.size}")
    purchaseOrder.copy(items = dedupedItems)
  }

  def selectPurchaseOrderFlow: Flow[ByteString, ByteString, NotUsed] =
    JsonFraming.objectScanner(Int.MaxValue)
  
  def convertByteToStringFlow(parallelism: Int): Flow[ByteString, String, NotUsed] =
    Flow[ByteString].mapAsync(parallelism) { s =>
      logger.info("Starting extracting Dump file")
      Future(s.utf8String)
    }

  def parseStringToPurchaseOrderFlow(parallelism: Int): Flow[String, PurchaseOrder , NotUsed] =
    Flow[String].mapAsync(parallelism) { s =>
      Future{
        logger.info("Processing Boomm")
        throw new Exception("Boom!!")
        PurchaseOrderParser.decodePurchaseOrder(s) match {
          case Right(po) =>
            logger.info(s"Successful parse PurchaseOrder Payload PO#: ${po.purchaseOrderNumber} EventId: ${po.salesEventId} ")
            po
          case Left(e) =>
            logger.error(s"Failed parsing of PurchaseOrder Payload ${e}")
            throw new Exception(e)
        }
      }
    }
  
  def dedupedPurchaseOrderFlow(parallelism: Int): Flow[PurchaseOrder, PurchaseOrder, NotUsed] =
    Flow[PurchaseOrder].mapAsync(parallelism) { s =>
      logger.info(s"Starting deduping PurchaseOrder PO#: ${s.purchaseOrderNumber} DID: ${s.departmentId} items count: ${s.items.size}")
      Future(dedupedPurchaseOrder(s))
    }

  def updateInventoryQuantity(parallelism: Int): Flow[PurchaseOrder, PurchaseOrder, NotUsed] =
    Flow[PurchaseOrder].mapAsync(parallelism) { po =>
      runUpdateInventory(po).unsafeToFuture()
    }
  
  def runUpdateInventory(po: PurchaseOrder):IO[PurchaseOrder] = {
      val runUpdateInventoryIO = IO {
        po.salesEventId match {
          case Some(eventId) =>
            // Each branch return IO
            val makeInventoryChanges = IO {
              logger.info(s"Update Inventory Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber} DID: ${po.departmentId} EID: ${eventId}")
              val inventoryChanges: List[InventoryChange] = po.items.map(i => InventoryChange(available = i.quantityOrdered, sku = i.sku, eventId = eventId))
              logger.info(s"InventoryChanges: $inventoryChanges")
              inventoryChanges
            }
            val updateInventoryProg = for {
              inventoryChanges <- makeInventoryChanges
              itemsFromDb <- inventoryDao.findInventoryByEventId(eventId)
              mergedItems = mergeInventoryItems(itemsFromDb, inventoryChanges)
              updatedRows <- inventoryDao.update(mergedItems)
            } yield {
              updatedRows match {
                case rows if rows > 0 =>
                  logger.info(s"Update Inventory Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber}. Rows# = ${rows} updated.")
                case _ =>
                  logger.info(s"Update Inventory Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber}. Zero updated.")
              }
              po
            }
            updateInventoryProg
          case None =>
            IO {
              logger.error(s"Failed Update Inventory Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber} DID: ${po.departmentId}")
              throw new Exception(s"Failed Update Inventory Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber} DID: ${po.departmentId}: Missing Sales Event Id")
            }
        }
      }
      runUpdateInventoryIO.flatten
  }
  
  def updateInboundOrderItemQuantity(parallelism: Int): Flow[PurchaseOrder, PurchaseOrder, NotUsed] =
    Flow[PurchaseOrder].mapAsync(parallelism) { po =>
      logger.info(s"Update Inbound Order Item Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber} DID: ${po.departmentId}")
      updateInboundOrderItemQuantityAction(po).unsafeToFuture()
    }
  
  private def updateInboundOrderItemQuantityAction(po: PurchaseOrder): IO[PurchaseOrder] = {
    val updateInboundOrderItemProg = for {
      itemChanges <- IO(po.items.map(i => InboundOrderItemChange(i.quantityOrdered, i.epmSku, po.purchaseOrderNumber)))
      itemsFromDb <- inboundOrderItemDao.getInboundOrderItems(po.purchaseOrderNumber)
      mergedItems = mergeInboundOrderItems(itemsFromDb, itemChanges)
      updatedRows <- inboundOrderItemDao.update(mergedItems)
    } yield {
      updatedRows match {
        case rows if rows > 0 =>
          logger.info(s"Update Inbound Order Item Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber}. Rows# = ${rows} updated.")
        case _ =>
          logger.info(s"Update Inbound Order Item Quantity PurchaseOrder PO#: ${po.purchaseOrderNumber}. Zero updated.")
      }
      po
    }
    updateInboundOrderItemProg
  }
  
  // Only get inbound order items exists in the database
  private def mergeInboundOrderItems(itemsFromDB: List[InboundOrderItemChange], itemsChange: List[InboundOrderItemChange]): List[InboundOrderItemChange] = {
    val newChanges = for {
      itemFromDB <- itemsFromDB
    } yield itemsChange.filter(_.similar(itemFromDB))
    newChanges.flatten
  }

  private def mergeInventoryItems(itemsFromDB: List[InventoryChange], itemsChange: List[InventoryChange]): List[InventoryChange] = {
    val newChanges = for {
      itemFromDB <- itemsFromDB
    } yield itemsChange.filter(_.similar(itemFromDB))
    logger.info(s"itemsFromDB: $itemsFromDB AND itemsChange: $itemsChange")
    newChanges.flatten
  }

  override def run(): IO[_] = {
    val ioFuture = IO.fromFuture(IO {
      jsonFileSource
        .via(selectPurchaseOrderFlow)
        .via(convertByteToStringFlow(purchaseOrderParallelism))
        .via(parseStringToPurchaseOrderFlow(purchaseOrderParallelism))
        .via(dedupedPurchaseOrderFlow(purchaseOrderParallelism))
        .via(updateInventoryQuantity(purchaseOrderParallelism))
        .via(updateInboundOrderItemQuantity(purchaseOrderParallelism))
        .log("Stepppp Boom")
        .withAttributes(supervisionStrategy(Supervision.stoppingDecider))
        .to(Sink.ignore).run()
    }).attempt.map {
      res => res match {
        case Right(value) =>
          logger.info(s"Looggging update: ${value}")
          value
        case Left(e) =>
          logger.error("ERRORRRR: ", e)
          throw e
      }
    }
    ioFuture
  }
  
  
}

object JsonConsumer{
  def apply(appConfig: AppConfig, inventoryDao: InventoryDao[IO], inboundOrderItemDao: InboundOrderItemDao[IO]): JsonConsumer = 
    new JsonConsumer(appConfig, inventoryDao, inboundOrderItemDao)
}
