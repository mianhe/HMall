package com.hmall.activity.application;

import java.time.Instant;

public record RecordActivityCommand(
    String eventId,
    String eventType,
    String topic,
    Long orderId,
    String payload,
    Instant occurredAt
) {}
