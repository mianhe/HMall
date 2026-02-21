package com.hmall.activity.domain;

public record ActivityStats(
    int ordersCreated,
    int ordersCancelled,
    int ordersCompleted,
    int paymentSuccess,
    int paymentFailed,
    int paymentExpired,
    long paymentTotalCents,
    int stockReserved,
    int stockReleased,
    int fulfillmentCreated,
    int fulfillmentAllocated,
    int fulfillmentShipped,
    int fulfillmentDelivered
) {
    public int paymentAttempts() {
        return paymentSuccess + paymentFailed + paymentExpired;
    }
}
