package com.hmall.activity.domain;

public record ActivityStats(
    int ordersCreated,
    int ordersCancelled,
    int ordersCompleted,
    int paymentSuccess,
    int paymentFailed,
    int paymentExpired,
    int stockReserved,
    int stockReleased
) {
    public int paymentAttempts() {
        return paymentSuccess + paymentFailed + paymentExpired;
    }
}
