package com.hmall.fulfillment.domain;

public class FulfillmentItem {

    private Long fulfillmentItemId;
    private final Long skuId;
    private final int quantity;

    public FulfillmentItem(Long skuId, int quantity) {
        if (skuId == null) {
            throw new IllegalArgumentException("skuId must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public FulfillmentItem(Long fulfillmentItemId, Long skuId, int quantity) {
        this(skuId, quantity);
        this.fulfillmentItemId = fulfillmentItemId;
    }

    public Long getFulfillmentItemId() { return fulfillmentItemId; }
    public Long getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
}
