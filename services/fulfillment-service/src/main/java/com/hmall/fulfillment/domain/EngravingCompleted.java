package com.hmall.fulfillment.domain;

import java.time.Instant;

public record EngravingCompleted(
    long orderId,
    long fulfillmentOrderId,
    Instant occurredAt
) {}
