package com.hmall.activity.domain;

import java.time.Instant;
import java.time.LocalDate;

public record OrderFact(
    Long orderId,
    Long userId,
    long totalAmountCents,
    int itemCount,
    int totalQuantity,
    boolean hasEngraving,
    boolean hasWarranty,
    String currentStage,
    String cancelReason,
    boolean isAbnormal,
    Instant createdAt,
    Instant paidAt,
    Instant shippedAt,
    Instant deliveredAt,
    Instant completedAt,
    Instant cancelledAt,
    Long paymentDurationSec,
    Long fulfillmentDurationSec,
    LocalDate createdDate,
    int createdHour,
    String seedBatch
) {}
