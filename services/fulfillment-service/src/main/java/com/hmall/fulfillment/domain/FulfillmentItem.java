package com.hmall.fulfillment.domain;

public class FulfillmentItem {

    private Long fulfillmentItemId;
    private final Long skuId;
    private final int quantity;
    private final FulfillmentItemType itemType;

    public FulfillmentItem(Long skuId, int quantity, FulfillmentItemType itemType) {
        if (skuId == null) {
            throw new IllegalArgumentException("skuId must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (itemType == null) {
            throw new IllegalArgumentException("itemType must not be null");
        }
        this.skuId = skuId;
        this.quantity = quantity;
        this.itemType = itemType;
    }

    public FulfillmentItem(Long fulfillmentItemId, Long skuId, int quantity, FulfillmentItemType itemType) {
        this(skuId, quantity, itemType);
        this.fulfillmentItemId = fulfillmentItemId;
    }

    public Long getFulfillmentItemId() { return fulfillmentItemId; }
    public Long getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public FulfillmentItemType getItemType() { return itemType; }
}
