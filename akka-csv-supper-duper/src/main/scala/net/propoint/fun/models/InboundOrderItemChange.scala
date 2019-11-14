package net.propoint.fun.models

case class InboundOrderItemChange(
  quantity: Int,
  epmSkuId: Long,
  inboundOrderId: Long
){
  def similar(that: InboundOrderItemChange): Boolean =
    this.epmSkuId.equals(that.epmSkuId) && this.inboundOrderId.equals(that.inboundOrderId)
}
