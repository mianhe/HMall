package com.hmall.activity.domain;

import java.time.Instant;

/**
 * 业务活动记录（聚合根）。来自各 BC 的领域事件，落库后供查询与统计。
 */
public record BusinessActivity(
    Long id,
    String eventId,
    String eventType,
    String topic,
    Long orderId,
    String payload,
    Instant occurredAt,
    Instant receivedAt
) {
    public static BusinessActivity of(
        String eventId,
        String eventType,
        String topic,
        Long orderId,
        String payload,
        Instant occurredAt,
        Instant receivedAt
    ) {
        return new BusinessActivity(null, eventId, eventType, topic, orderId, payload, occurredAt, receivedAt);
    }
}
