package com.hmall.order.application.event;

/**
 * 订单行快照（用于出站事件 payload）。含 skuId、spuId、数量与单价快照，供多流程分析使用。
 * 来自业务需求：智能运营 Step 1。
 */
public record ItemSnapshot(long skuId, Long spuId, int quantity, long unitPriceCents) {}
