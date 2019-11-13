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
    logger.info(s"Updating Inventory: ${inboundOrderItemChanges.size}")
    DoobieHelpers.batchUpdate(catalogWriteConn.xa, inboundOrderItemChanges, insertInboundOrderSql).map(_.sum)
  }
}

private object InboundOrderItemUpdateSql {
  
  def insertInboundOrderSql: Update[InboundOrderItemChange] = {
    val query = """INSERT INTO inbound_orders_items (epm_sku_id, inbound_order_id, quantity)
                  |VALUES (?, ?, ?)
                  |ON DUPLICATE KEY UPDATE quantity=VALUES(quantity);"""
      .stripMargin
    Update[InboundOrderItemChange](query)
  }
  
}
