package com.hmall.activity.infrastructure.persistence;

import com.hmall.activity.domain.BusinessActivity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "business_activity")
public class BusinessActivityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String topic;

    @Column(nullable = true)
    private Long orderId;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant receivedAt;

    public static BusinessActivityEntity from(BusinessActivity domain) {
        BusinessActivityEntity e = new BusinessActivityEntity();
        e.eventId = domain.eventId();
        e.eventType = domain.eventType();
        e.topic = domain.topic();
        e.orderId = domain.orderId();
        e.payload = domain.payload();
        e.occurredAt = domain.occurredAt();
        e.receivedAt = domain.receivedAt();
        return e;
    }

    public BusinessActivity toDomain() {
        return new BusinessActivity(id, eventId, eventType, topic, orderId, payload, occurredAt, receivedAt);
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTopic() { return topic; }
    public Long getOrderId() { return orderId; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getReceivedAt() { return receivedAt; }
}
