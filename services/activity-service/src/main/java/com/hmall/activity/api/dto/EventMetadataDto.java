package com.hmall.activity.api.dto;

import com.hmall.activity.domain.EventMetadata;

public record EventMetadataDto(
    String eventType,
    String boundedContext,
    String label,
    String category,
    String compensatesEventType
) {
    public static EventMetadataDto from(EventMetadata m) {
        return new EventMetadataDto(
            m.eventType(),
            m.boundedContext(),
            m.label(),
            m.category().name().toLowerCase(),
            m.compensatesEventType()
        );
    }
}
