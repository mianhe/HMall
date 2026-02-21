package com.hmall.fulfillment.domain;

import java.time.Instant;

public record FulfillmentOrderAllocated(
    long orderId,
    long fulfillmentOrderId,
    Instant occurredAt
) {}
