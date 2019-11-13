package net.propoint.fun.daos

import net.propoint.fun.models.InboundOrderItemChange

trait InboundOrderItemDao[F[_]] {
  def update(inboundOrderItemChanges: List[InboundOrderItemChange]): F[Int]
}
