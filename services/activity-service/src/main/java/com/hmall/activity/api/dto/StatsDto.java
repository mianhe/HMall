package com.hmall.activity.api.dto;

import com.hmall.activity.domain.ActivityStats;

import java.time.LocalDate;

public record StatsDto(
    int ordersCreated,
    int ordersCancelled,
    int ordersCompleted,
    int paymentAttempts,
    int paymentSuccess,
    int paymentFailed,
    int paymentExpired,
    int stockReserved,
    int stockReleased,
    String from,
    String to
) {
    public static StatsDto from(ActivityStats s, LocalDate fromDate, LocalDate toDate) {
        return new StatsDto(
            s.ordersCreated(), s.ordersCancelled(), s.ordersCompleted(),
            s.paymentAttempts(), s.paymentSuccess(), s.paymentFailed(), s.paymentExpired(),
            s.stockReserved(), s.stockReleased(),
            fromDate.toString(), toDate.toString()
        );
    }
}
