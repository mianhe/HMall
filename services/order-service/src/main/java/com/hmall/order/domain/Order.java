package com.hmall.order.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Order {

    private final Long orderId;
    private final Long userId;
    private final OrderStatus status;
    private final long totalAmountCents;
    private final ShippingAddress shippingAddress;
    private final List<OrderLineItem> items;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Order(Long userId, ShippingAddress shippingAddress, List<OrderLineItem> items) {
        this.orderId = null;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalAmountCents = items.stream().mapToLong(OrderLineItem::getTotalPriceCents).sum();
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "shippingAddress");
        this.items = new ArrayList<>(items);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Order(Long orderId, Long userId, OrderStatus status, long totalAmountCents,
                 ShippingAddress shippingAddress, List<OrderLineItem> items, Instant createdAt, Instant updatedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.totalAmountCents = totalAmountCents;
        this.shippingAddress = shippingAddress;
        this.items = new ArrayList<>(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public long getTotalAmountCents() { return totalAmountCents; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public List<OrderLineItem> getItems() { return List.copyOf(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
