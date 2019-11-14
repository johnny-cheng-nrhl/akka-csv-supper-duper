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

class InventoryDaoImpl[F[_]: Monad: Par](ecommWriteConn: DoobieConnections.Write[F]) 
  extends InventoryDao[F] with LazyLogging {
  
  import InventoryUpdateSql._
  
  override def update(inventoryChanges: List[InventoryChange]): F[Int] = {
    logger.info(s"Updating Inventory rows size: ${inventoryChanges.size}")
    DoobieHelpers.batchUpdate(ecommWriteConn.xa, inventoryChanges, insertInventorySql).map(_.sum)
  }

  override def findInventoryByEventId(eventId: Long): F[List[InventoryChange]] = {
    logger.info(s"Retrieving Inventory with EventID: ${eventId}")
    getInventoryByEventId(eventId).to[List].transact(ecommWriteConn.xa)
  }
}

private object InventoryUpdateSql {
  
  def getInventoryByEventId(eventId: Long): Query0[InventoryChange] =
    sql"""SELECT sku AS sku, event_id AS eventId, available AS available
          FROM inventory WHERE event_id = $eventId""".query[InventoryChange]
  
  def updateAvaiable(change: InventoryChange): Update0 = {
    fr"""UPDATE FROM inventory i 
          SET i.available = ${change.available} WHERE i.event_id = ${change.eventId}
          AND i.sku = ${change.sku}""".stripMargin.update
  }

  def insertInventorySql: Update[InventoryChange] = {
    val query = """INSERT INTO inventory (sku, event_id, available)
                  |VALUES (?, ?, ?)
                  |ON DUPLICATE KEY UPDATE available=VALUES(available);"""
      .stripMargin
    Update[InventoryChange](query)
  }
}
