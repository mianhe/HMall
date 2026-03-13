package com.hmall.activity.api.dto;

import com.hmall.activity.domain.EventMetadata;

import java.util.Map;

public record EventMetadataDto(
    String eventType,
    String boundedContext,
    String label,
    String category,
    String compensatesEventType,
    String origin,
    Map<String, String> processRoles
) {
    public static EventMetadataDto from(EventMetadata m) {
        return new EventMetadataDto(
            m.eventType(),
            m.boundedContext(),
            m.label(),
            m.category().name().toLowerCase(),
            m.compensatesEventType(),
            m.origin().name().toLowerCase(),
            m.processRoles() != null ? Map.copyOf(m.processRoles()) : Map.of()
        );
    }
}
