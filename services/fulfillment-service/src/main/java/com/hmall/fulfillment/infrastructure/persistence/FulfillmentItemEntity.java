package com.hmall.fulfillment.infrastructure.persistence;

import com.hmall.fulfillment.domain.FulfillmentItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fulfillment_item")
public class FulfillmentItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "item_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FulfillmentItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfillment_order_id", nullable = false)
    private FulfillmentOrderEntity fulfillmentOrder;

    public FulfillmentItemEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public FulfillmentItemType getItemType() { return itemType; }
    public void setItemType(FulfillmentItemType itemType) { this.itemType = itemType; }
    public FulfillmentOrderEntity getFulfillmentOrder() { return fulfillmentOrder; }
    public void setFulfillmentOrder(FulfillmentOrderEntity fulfillmentOrder) { this.fulfillmentOrder = fulfillmentOrder; }
}
