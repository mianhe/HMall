package com.hmall.activity.api.dto;

import com.hmall.activity.domain.BusinessActivity;
import com.hmall.activity.domain.EventMetadataRegistry;

import java.time.Instant;

public record ActivityDto(
    Long id,
    String eventId,
    String eventType,
    String topic,
    Long orderId,
    String payload,
    Instant occurredAt,
    Instant receivedAt,
    EventMetadataDto metadata
) {
    public static ActivityDto from(BusinessActivity a) {
        EventMetadataDto meta = EventMetadataRegistry.find(a.eventType())
            .map(EventMetadataDto::from)
            .orElse(null);
        return new ActivityDto(
            a.id(),
            a.eventId(),
            a.eventType(),
            a.topic(),
            a.orderId(),
            a.payload(),
            a.occurredAt(),
            a.receivedAt(),
            meta
        );
    }
}
