package net.propoint.fun.models

case class InventoryChange(available: Int, sku: Long, eventId: Long){
  def similar(that: InventoryChange): Boolean =
    this.sku.equals(that.sku) && this.eventId.equals(that.eventId)
}
