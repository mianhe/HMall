package com.hmall.activity.domain;

import java.util.Map;

/**
 * 事件元数据：描述一种事件类型的业务语义，包括所属 BC、显示标签、分类、补偿关系、
 * 来源类型（domain/behavioral/derived）及一级流程角色（智能运营 Step 1）。
 */
public record EventMetadata(
    String eventType,
    String boundedContext,
    String label,
    EventCategory category,
    String compensatesEventType,
    EventOrigin origin,
    Map<String, String> processRoles
) {
    public static EventMetadata normal(String eventType, String boundedContext, String label) {
        return normal(eventType, boundedContext, label, EventOrigin.DOMAIN, Map.of());
    }

    public static EventMetadata normal(String eventType, String boundedContext, String label,
                                       EventOrigin origin, Map<String, String> processRoles) {
        return new EventMetadata(eventType, boundedContext, label, EventCategory.NORMAL, null, origin, processRoles);
    }

    public static EventMetadata compensation(String eventType, String boundedContext, String label, String compensatesEventType) {
        return compensation(eventType, boundedContext, label, compensatesEventType, EventOrigin.DOMAIN, Map.of());
    }

    public static EventMetadata compensation(String eventType, String boundedContext, String label,
                                             String compensatesEventType, EventOrigin origin, Map<String, String> processRoles) {
        return new EventMetadata(eventType, boundedContext, label, EventCategory.COMPENSATION, compensatesEventType, origin, processRoles);
    }

    public static EventMetadata exception(String eventType, String boundedContext, String label) {
        return exception(eventType, boundedContext, label, EventOrigin.DOMAIN, Map.of());
    }

    public static EventMetadata exception(String eventType, String boundedContext, String label,
                                          EventOrigin origin, Map<String, String> processRoles) {
        return new EventMetadata(eventType, boundedContext, label, EventCategory.EXCEPTION, null, origin, processRoles);
    }
}
