package com.hmall.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    FULFILLING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED
}
