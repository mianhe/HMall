package com.hmall.fulfillment.domain;

import java.time.Instant;
import java.util.List;

public record FulfillmentOrderCreated(
    long orderId,
    List<Long> fulfillmentOrderIds,
    Instant occurredAt
) {}
