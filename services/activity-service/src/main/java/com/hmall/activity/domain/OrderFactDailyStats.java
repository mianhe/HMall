package com.hmall.activity.domain;

import java.time.LocalDate;

public record OrderFactDailyStats(
    LocalDate date,
    int totalOrders,
    int completedOrders,
    int cancelledOrders,
    int vasOrders,
    int engravingOrders,
    int warrantyOrders,
    long totalRevenueCents,
    long avgAmountCents
) {}
