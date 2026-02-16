package com.hmall.inventory.domain;

import java.time.Instant;

/**
 * 领域事件：库存已释放。release 成功时发布。
 */
public record StockReleased(
    long orderId,
    Instant occurredAt
) {}
