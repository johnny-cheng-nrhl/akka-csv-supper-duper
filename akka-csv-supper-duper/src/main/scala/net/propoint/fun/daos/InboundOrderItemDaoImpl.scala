package net.propoint.fun.daos

import cats._
import cats.implicits._
import cats.temp.par.Par
import doobie._
import doobie.implicits._
import com.typesafe.scalalogging.LazyLogging
import net.propoint.fun.config.DoobieConnections
import net.propoint.fun.models.InboundOrderItemChange
import net.propoint.fun.platform.DoobieHelpers

class InboundOrderItemDaoImpl[F[_]: Monad: Par](catalogWriteConn: DoobieConnections.Write[F])
  extends InboundOrderItemDao[F] with LazyLogging {
  
  import InboundOrderItemUpdateSql._
  
  override def update(inboundOrderItemChanges: List[InboundOrderItemChange]): F[Int] = {
    logger.info(s"Updating Inbound Order Items rows size: ${inboundOrderItemChanges.size}")
    DoobieHelpers.batchUpdate(catalogWriteConn.xa, inboundOrderItemChanges, updateInboundOrderSql).map(_.sum)
  }

  override def getInboundOrderItems(inboundOrderId: Long): F[List[InboundOrderItemChange]] = {
    logger.info(s"Retrieving Inbound Order Items for InboundOrderId: ${inboundOrderId}")
    findInboundOrderItemsByInboundOrderId(inboundOrderId).to[List].transact(catalogWriteConn.xa)
  }
}

private object InboundOrderItemUpdateSql {
  
  def findInboundOrderItemsByInboundOrderId(inboundOrderId: Long): Query0[InboundOrderItemChange] =
    sql"""SELECT epm_sku_id AS epmSkuId, inbound_order_id AS inboundOrderId, quantity AS quantity
          FROM inbound_orders_items WHERE inbound_order_id = $inboundOrderId
          """.query[InboundOrderItemChange]
  
  def updateInboundOrderSql: Update[InboundOrderItemChange] = {
    val query =
      """
        |UPDATE inbound_orders_items SET quantity = ? WHERE epm_sku_id = ?
        |AND inbound_order_id = ?
        |""".stripMargin
    Update[InboundOrderItemChange](query)
  }
  
//  def insertInboundOrderSql: Update[InboundOrderItemChange] = {
//    val query = """INSERT INTO inbound_orders_items (quantity, epm_sku_id, inbound_order_id)
//                  |VALUES (?, ?, ?)
//                  |ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);"""
//      .stripMargin
//    Update[InboundOrderItemChange](query)
//  }
  
}
