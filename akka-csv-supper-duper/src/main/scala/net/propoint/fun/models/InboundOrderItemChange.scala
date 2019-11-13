package net.propoint.fun.models

case class InboundOrderItemChange(
  epmSkuId: Long,
  inboundOrderId: Long,
  quantity: Int
)
