package com.hmall.fulfillment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FulfillmentOrder {

    private Long fulfillmentOrderId;
    private final Long orderId;
    private final FulfillmentType fulfillmentType;
    private FulfillmentOrderStatus status;
    private final List<FulfillmentItem> items;
    private final ShippingAddress shippingAddress;
    private ShippingInfo shippingInfo;
    private final EngravingInfo engravingInfo;
    private Instant engravingCompletedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public FulfillmentOrder(Long orderId, FulfillmentType fulfillmentType, List<FulfillmentItem> items, ShippingAddress shippingAddress) {
        this(orderId, fulfillmentType, items, shippingAddress, null);
    }

    public FulfillmentOrder(Long orderId, FulfillmentType fulfillmentType, List<FulfillmentItem> items, ShippingAddress shippingAddress, EngravingInfo engravingInfo) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.fulfillmentType = Objects.requireNonNull(fulfillmentType, "fulfillmentType");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
        this.items = new ArrayList<>(items);
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "shippingAddress");
        this.engravingInfo = engravingInfo;
        this.engravingCompletedAt = null;
        this.status = FulfillmentOrderStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 从持久化还原 */
    public FulfillmentOrder(Long fulfillmentOrderId, Long orderId, FulfillmentType fulfillmentType, FulfillmentOrderStatus status,
                            List<FulfillmentItem> items, ShippingAddress shippingAddress,
                            ShippingInfo shippingInfo, EngravingInfo engravingInfo, Instant engravingCompletedAt,
                            Instant createdAt, Instant updatedAt) {
        this.fulfillmentOrderId = fulfillmentOrderId;
        this.orderId = orderId;
        this.fulfillmentType = fulfillmentType;
        this.status = status;
        this.items = new ArrayList<>(items);
        this.shippingAddress = shippingAddress;
        this.shippingInfo = shippingInfo;
        this.engravingInfo = engravingInfo;
        this.engravingCompletedAt = engravingCompletedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void allocate() {
        if (fulfillmentType == FulfillmentType.VIRTUAL) {
            throw new IllegalStateException("虚拟履约单不支持配货");
        }
        if (status != FulfillmentOrderStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 状态可以开始配货，当前状态: " + status);
        }
        this.status = FulfillmentOrderStatus.ALLOCATING;
        this.updatedAt = Instant.now();
    }

    public void completeEngraving() {
        if (fulfillmentType == FulfillmentType.VIRTUAL) {
            throw new IllegalStateException("虚拟履约单无镭雕");
        }
        if (engravingInfo == null) {
            throw new IllegalStateException("无镭雕内容，无法执行完成镭雕");
        }
        if (engravingCompletedAt != null) {
            throw new IllegalStateException("镭雕已完成，无法重复操作");
        }
        this.engravingCompletedAt = Instant.now();
        this.updatedAt = this.engravingCompletedAt;
    }

    public void ship(String carrier, String trackingNumber) {
        if (fulfillmentType == FulfillmentType.VIRTUAL) {
            throw new IllegalStateException("虚拟履约单不支持发货");
        }
        if (status != FulfillmentOrderStatus.ALLOCATING) {
            throw new IllegalStateException("只有 ALLOCATING 状态可以发货，当前状态: " + status);
        }
        if (engravingInfo != null && engravingCompletedAt == null) {
            throw new IllegalStateException("有镭雕内容，须先完成镭雕后才能发货");
        }
        this.shippingInfo = new ShippingInfo(carrier, trackingNumber, Instant.now());
        this.status = FulfillmentOrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
    }

    public void confirmDelivery() {
        if (fulfillmentType == FulfillmentType.VIRTUAL) {
            throw new IllegalStateException("虚拟履约单不支持签收");
        }
        if (status != FulfillmentOrderStatus.SHIPPED) {
            throw new IllegalStateException("只有 SHIPPED 状态可以签收，当前状态: " + status);
        }
        this.shippingInfo.markDelivered(Instant.now());
        this.status = FulfillmentOrderStatus.DELIVERED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (fulfillmentType != FulfillmentType.VIRTUAL) {
            throw new IllegalStateException("只有虚拟履约单可以激活");
        }
        if (status != FulfillmentOrderStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 状态可以激活，当前状态: " + status);
        }
        this.status = FulfillmentOrderStatus.ACTIVATED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (status != FulfillmentOrderStatus.CREATED && status != FulfillmentOrderStatus.ALLOCATING) {
            throw new IllegalStateException("只有 CREATED 或 ALLOCATING 状态可以取消，当前状态: " + status);
        }
        this.status = FulfillmentOrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public Long getFulfillmentOrderId() { return fulfillmentOrderId; }
    public Long getOrderId() { return orderId; }
    public FulfillmentType getFulfillmentType() { return fulfillmentType; }
    public FulfillmentOrderStatus getStatus() { return status; }
    public List<FulfillmentItem> getItems() { return Collections.unmodifiableList(items); }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public ShippingInfo getShippingInfo() { return shippingInfo; }
    public EngravingInfo getEngravingInfo() { return engravingInfo; }
    public Instant getEngravingCompletedAt() { return engravingCompletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
