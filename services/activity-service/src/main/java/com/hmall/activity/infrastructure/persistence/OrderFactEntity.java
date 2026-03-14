package com.hmall.activity.infrastructure.persistence;

import com.hmall.activity.domain.OrderFact;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "order_fact")
public class OrderFactEntity {

    @Id
    private Long orderId;

    private Long userId;
    private long totalAmountCents;
    private int itemCount;
    private int totalQuantity;
    private boolean hasEngraving;
    private boolean hasWarranty;

    @Column(length = 32, nullable = false)
    private String currentStage;

    @Column(length = 32)
    private String cancelReason;

    private boolean isAbnormal;
    private Instant createdAt;
    private Instant paidAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Long paymentDurationSec;
    private Long fulfillmentDurationSec;
    private LocalDate createdDate;
    private int createdHour;

    @Column(length = 64)
    private String seedBatch;

    public static OrderFactEntity from(OrderFact fact) {
        OrderFactEntity e = new OrderFactEntity();
        e.orderId = fact.orderId();
        e.userId = fact.userId();
        e.totalAmountCents = fact.totalAmountCents();
        e.itemCount = fact.itemCount();
        e.totalQuantity = fact.totalQuantity();
        e.hasEngraving = fact.hasEngraving();
        e.hasWarranty = fact.hasWarranty();
        e.currentStage = fact.currentStage();
        e.cancelReason = fact.cancelReason();
        e.isAbnormal = fact.isAbnormal();
        e.createdAt = fact.createdAt();
        e.paidAt = fact.paidAt();
        e.shippedAt = fact.shippedAt();
        e.deliveredAt = fact.deliveredAt();
        e.completedAt = fact.completedAt();
        e.cancelledAt = fact.cancelledAt();
        e.paymentDurationSec = fact.paymentDurationSec();
        e.fulfillmentDurationSec = fact.fulfillmentDurationSec();
        e.createdDate = fact.createdDate();
        e.createdHour = fact.createdHour();
        e.seedBatch = fact.seedBatch();
        return e;
    }

    public OrderFact toDomain() {
        return new OrderFact(orderId, userId, totalAmountCents, itemCount, totalQuantity,
            hasEngraving, hasWarranty, currentStage, cancelReason, isAbnormal,
            createdAt, paidAt, shippedAt, deliveredAt, completedAt, cancelledAt,
            paymentDurationSec, fulfillmentDurationSec, createdDate, createdHour, seedBatch);
    }

    public Long getOrderId() { return orderId; }
}
