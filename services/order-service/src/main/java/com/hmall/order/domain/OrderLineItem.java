package com.hmall.order.domain;

import java.util.Objects;

public class OrderLineItem {

    private final Long lineItemId;
    private final Long orderId;
    private final Long skuId;
    private final int quantity;
    private final long unitPriceCents;
    private final long totalPriceCents;
    private final String displayName;

    public OrderLineItem(Long skuId, int quantity, long unitPriceCents, String displayName) {
        this.lineItemId = null;
        this.orderId = null;
        this.skuId = Objects.requireNonNull(skuId, "skuId");
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.totalPriceCents = (long) quantity * unitPriceCents;
        this.displayName = displayName;
    }

    public OrderLineItem(Long lineItemId, Long orderId, Long skuId, int quantity, long unitPriceCents, long totalPriceCents, String displayName) {
        this.lineItemId = lineItemId;
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.totalPriceCents = totalPriceCents;
        this.displayName = displayName;
    }

    public Long getLineItemId() { return lineItemId; }
    public Long getOrderId() { return orderId; }
    public Long getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public long getTotalPriceCents() { return totalPriceCents; }
    public String getDisplayName() { return displayName; }
}
