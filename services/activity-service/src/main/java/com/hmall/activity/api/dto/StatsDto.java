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
    long paymentTotalCents,
    int stockReserved,
    int stockReleased,
    int fulfillmentCreated,
    int fulfillmentAllocated,
    int fulfillmentShipped,
    int fulfillmentDelivered,
    long distinctBuyerCount,
    String from,
    String to
) {
    public static StatsDto from(ActivityStats s, LocalDate fromDate, LocalDate toDate) {
        return new StatsDto(
            s.ordersCreated(), s.ordersCancelled(), s.ordersCompleted(),
            s.paymentAttempts(), s.paymentSuccess(), s.paymentFailed(), s.paymentExpired(),
            s.paymentTotalCents(),
            s.stockReserved(), s.stockReleased(),
            s.fulfillmentCreated(), s.fulfillmentAllocated(), s.fulfillmentShipped(), s.fulfillmentDelivered(),
            s.distinctBuyerCount(),
            fromDate.toString(), toDate.toString()
        );
    }
}
