package com.hmall.fulfillment.domain;

import java.time.Instant;

public record ServiceActivated(
    long orderId,
    long fulfillmentOrderId,
    long serviceSkuId,
    Instant activatedAt,
    Instant expiresAt,
    Instant occurredAt
) {}
