package com.hmall.fulfillment.domain;

import java.time.Instant;

public record FulfillmentDelivered(
    long orderId,
    long fulfillmentOrderId,
    Instant occurredAt
) {}
