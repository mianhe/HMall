package com.hmall.activity.domain;

/**
 * 事件元数据：描述一种事件类型的业务语义，包括所属 BC、显示标签、分类（正向/补偿/异常）、
 * 以及补偿关系（补偿事件补偿了哪个正向事件）。
 */
public record EventMetadata(
    String eventType,
    String boundedContext,
    String label,
    EventCategory category,
    String compensatesEventType
) {
    public static EventMetadata normal(String eventType, String boundedContext, String label) {
        return new EventMetadata(eventType, boundedContext, label, EventCategory.NORMAL, null);
    }

    public static EventMetadata compensation(String eventType, String boundedContext, String label, String compensatesEventType) {
        return new EventMetadata(eventType, boundedContext, label, EventCategory.COMPENSATION, compensatesEventType);
    }

    public static EventMetadata exception(String eventType, String boundedContext, String label) {
        return new EventMetadata(eventType, boundedContext, label, EventCategory.EXCEPTION, null);
    }
}
