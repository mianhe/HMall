package com.hmall.fulfillment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FulfillmentOrder {

    private Long fulfillmentOrderId;
    private final Long orderId;
    private FulfillmentOrderStatus status;
    private final List<FulfillmentItem> items;
    private final ShippingAddress shippingAddress;
    private ShippingInfo shippingInfo;
    private final Instant createdAt;
    private Instant updatedAt;

    public FulfillmentOrder(Long orderId, List<FulfillmentItem> items, ShippingAddress shippingAddress) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        this.items = new ArrayList<>(items);
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "shippingAddress");
        this.status = FulfillmentOrderStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 从持久化还原 */
    public FulfillmentOrder(Long fulfillmentOrderId, Long orderId, FulfillmentOrderStatus status,
                            List<FulfillmentItem> items, ShippingAddress shippingAddress,
                            ShippingInfo shippingInfo, Instant createdAt, Instant updatedAt) {
        this.fulfillmentOrderId = fulfillmentOrderId;
        this.orderId = orderId;
        this.status = status;
        this.items = new ArrayList<>(items);
        this.shippingAddress = shippingAddress;
        this.shippingInfo = shippingInfo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void ship(String carrier, String trackingNumber) {
        if (status != FulfillmentOrderStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 状态可以发货，当前状态: " + status);
        }
        this.shippingInfo = new ShippingInfo(carrier, trackingNumber, Instant.now());
        this.status = FulfillmentOrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
    }

    public void confirmDelivery() {
        if (status != FulfillmentOrderStatus.SHIPPED) {
            throw new IllegalStateException("只有 SHIPPED 状态可以签收，当前状态: " + status);
        }
        this.shippingInfo.markDelivered(Instant.now());
        this.status = FulfillmentOrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (status != FulfillmentOrderStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 状态可以取消，当前状态: " + status);
        }
        this.status = FulfillmentOrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public Long getFulfillmentOrderId() { return fulfillmentOrderId; }
    public Long getOrderId() { return orderId; }
    public FulfillmentOrderStatus getStatus() { return status; }
    public List<FulfillmentItem> getItems() { return Collections.unmodifiableList(items); }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public ShippingInfo getShippingInfo() { return shippingInfo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
