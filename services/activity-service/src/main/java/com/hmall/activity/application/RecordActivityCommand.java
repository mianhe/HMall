package com.hmall.activity.application;

import java.time.Instant;

public record RecordActivityCommand(
    String eventId,
    String eventType,
    String topic,
    Long orderId,
    Long userId,
    String correlationKeys,
    String payload,
    Instant occurredAt
) {}
