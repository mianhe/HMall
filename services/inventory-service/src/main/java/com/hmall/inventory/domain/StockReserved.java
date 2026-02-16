package com.hmall.inventory.domain;

import java.time.Instant;
import java.util.List;

/**
 * 领域事件：库存已占用。occupy 成功时发布。
 */
public record StockReserved(
    long orderId,
    List<OccupyItemPayload> items,
    Instant occurredAt
) {
    public record OccupyItemPayload(long skuId, int quantity) {}
}
