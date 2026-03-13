package com.hmall.activity.api.dto;

import com.hmall.activity.domain.DailyActivityStats;

public record DailyStatsDto(
    String date,
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
    long distinctBuyerCount
) {
    public static DailyStatsDto from(DailyActivityStats ds) {
        var s = ds.stats();
        return new DailyStatsDto(
            ds.date().toString(),
            s.ordersCreated(), s.ordersCancelled(), s.ordersCompleted(),
            s.paymentAttempts(), s.paymentSuccess(), s.paymentFailed(), s.paymentExpired(),
            s.paymentTotalCents(),
            s.stockReserved(), s.stockReleased(),
            s.fulfillmentCreated(), s.fulfillmentAllocated(), s.fulfillmentShipped(), s.fulfillmentDelivered(),
            s.distinctBuyerCount()
        );
    }
}
