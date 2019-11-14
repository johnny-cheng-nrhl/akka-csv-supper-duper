package net.propoint.fun.daos

import net.propoint.fun.models.InventoryChange

trait InventoryDao[F[_]] {
  def findInventoryByEventId(eventId: Long): F[List[InventoryChange]]
  def update(inventoryChanges: List[InventoryChange]): F[Int]
}
