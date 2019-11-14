package net.propoint.fun.daos

import net.propoint.fun.config._
import cats._
import cats.implicits._
import cats.temp.par.Par
import doobie._
import doobie.implicits._
import com.typesafe.scalalogging.LazyLogging
import net.propoint.fun.models.InventoryChange
import net.propoint.fun.platform.DoobieHelpers

class InventoryDaoImpl[F[_]: Par](ecommWriteConn: DoobieConnections.Write[F])(implicit me: MonadError[F, Throwable])
  extends InventoryDao[F] with LazyLogging {
  
  import InventoryUpdateSql._
  
  override def update(inventoryChanges: List[InventoryChange]): F[Int] = {
    logger.info(s"Updating Inventory rows size: ${inventoryChanges}")
    val result = (DoobieHelpers.batchUpdate(ecommWriteConn.xa, inventoryChanges, updateAvaiableSql).map(_.sum).map {
      i => logger.info(s"After update inventory rows: ${i}")
      i
    }).attempt.map {
      res => res match {
        case Right(value) =>
          logger.info(s"Dao Logiing update: ${value}")
          value
        case Left(e) =>
          logger.error("ERRORRRR: ", e)
          throw e
      }
    }
    result
  }

  override def findInventoryByEventId(eventId: Long): F[List[InventoryChange]] = {
    logger.info(s"Retrieving Inventory with EventID: ${eventId}")
    getInventoryByEventId(eventId).to[List].transact(ecommWriteConn.xa)
  }
}

private object InventoryUpdateSql {
  
  def getInventoryByEventId(eventId: Long): Query0[InventoryChange] =
    sql"""SELECT available AS available, sku AS sku, event_id AS eventId
          FROM inventory WHERE event_id = $eventId""".query[InventoryChange]
  
  def updateAvaiableSql: Update[InventoryChange]= {
    val query = """UPDATE inventory i 
          SET i.available = ? WHERE i.sku = ?
          AND i.event_id = ?""".stripMargin
    Update[InventoryChange](query)
  }

  def insertInventorySql: Update[InventoryChange] = {
    val query = """INSERT INTO inventory (available, sku, event_id)
                  |VALUES (?, ?, ?)
                  |ON DUPLICATE KEY UPDATE available=VALUES(available);"""
      .stripMargin
    Update[InventoryChange](query)
  }
}
