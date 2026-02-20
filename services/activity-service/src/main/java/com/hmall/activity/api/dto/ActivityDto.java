package com.hmall.activity.api.dto;

import com.hmall.activity.domain.BusinessActivity;

import java.time.Instant;

public record ActivityDto(
    Long id,
    String eventId,
    String eventType,
    String topic,
    Long orderId,
    String payload,
    Instant occurredAt,
    Instant receivedAt
) {
    public static ActivityDto from(BusinessActivity a) {
        return new ActivityDto(
            a.id(),
            a.eventId(),
            a.eventType(),
            a.topic(),
            a.orderId(),
            a.payload(),
            a.occurredAt(),
            a.receivedAt()
        );
    }
}
