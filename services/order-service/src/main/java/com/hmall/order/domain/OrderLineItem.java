package com.hmall.order.domain;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class OrderLineItem {

    private final Long lineItemId;
    private final Long orderId;
    private final Long skuId;
    private final int quantity;
    private final long unitPriceCents;
    private final long totalPriceCents;
    private final String displayName;
    private final OrderItemType itemType;
    private final Long relatedSkuId;
    private final Long spuId;
    private final Map<String, Object> serviceAttributes;

    public OrderLineItem(Long skuId, int quantity, long unitPriceCents, String displayName, OrderItemType itemType) {
        this(skuId, quantity, unitPriceCents, displayName, itemType, null, null, null);
    }

    public OrderLineItem(Long skuId, int quantity, long unitPriceCents, String displayName,
                          OrderItemType itemType, Long relatedSkuId, Long spuId) {
        this(skuId, quantity, unitPriceCents, displayName, itemType, relatedSkuId, spuId, null);
    }

    public OrderLineItem(Long skuId, int quantity, long unitPriceCents, String displayName,
                          OrderItemType itemType, Long relatedSkuId, Long spuId, Map<String, Object> serviceAttributes) {
        this.lineItemId = null;
        this.orderId = null;
        this.skuId = Objects.requireNonNull(skuId, "skuId");
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.totalPriceCents = (long) quantity * unitPriceCents;
        this.displayName = displayName;
        this.itemType = Objects.requireNonNull(itemType, "itemType");
        this.relatedSkuId = relatedSkuId;
        this.spuId = spuId;
        this.serviceAttributes = serviceAttributes != null ? Map.copyOf(serviceAttributes) : null;
    }

    public OrderLineItem(Long lineItemId, Long orderId, Long skuId, int quantity,
                          long unitPriceCents, long totalPriceCents, String displayName,
                          OrderItemType itemType, Long relatedSkuId, Long spuId) {
        this(lineItemId, orderId, skuId, quantity, unitPriceCents, totalPriceCents, displayName, itemType, relatedSkuId, spuId, null);
    }

    public OrderLineItem(Long lineItemId, Long orderId, Long skuId, int quantity,
                          long unitPriceCents, long totalPriceCents, String displayName,
                          OrderItemType itemType, Long relatedSkuId, Long spuId, Map<String, Object> serviceAttributes) {
        this.lineItemId = lineItemId;
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.totalPriceCents = totalPriceCents;
        this.displayName = displayName;
        this.itemType = itemType;
        this.relatedSkuId = relatedSkuId;
        this.spuId = spuId;
        this.serviceAttributes = serviceAttributes != null ? Map.copyOf(serviceAttributes) : null;
    }

    public Long getLineItemId() { return lineItemId; }
    public Long getOrderId() { return orderId; }
    public Long getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public long getTotalPriceCents() { return totalPriceCents; }
    public String getDisplayName() { return displayName; }
    public OrderItemType getItemType() { return itemType; }
    public Long getRelatedSkuId() { return relatedSkuId; }
    public Long getSpuId() { return spuId; }
    public Map<String, Object> getServiceAttributes() { return serviceAttributes != null ? Collections.unmodifiableMap(serviceAttributes) : null; }
}
