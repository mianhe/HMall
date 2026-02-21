package com.hmall.fulfillment.domain;

import java.time.Instant;

public record FulfillmentShipped(
    long orderId,
    long fulfillmentOrderId,
    Instant occurredAt
) {}
