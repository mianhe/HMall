package com.hmall.order.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "order_line_item")
public class OrderLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private Long unitPriceCents;

    @Column(name = "total_price_cents", nullable = false)
    private Long totalPriceCents;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "related_sku_id")
    private Long relatedSkuId;

    @Column(name = "spu_id")
    private Long spuId;

    @Column(name = "service_attributes", length = 2048)
    private String serviceAttributesJson;

    @Column(name = "discounts_json", length = 2048)
    private String discountsJson;

    public OrderLineItemEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(Long unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    public Long getTotalPriceCents() { return totalPriceCents; }
    public void setTotalPriceCents(Long totalPriceCents) { this.totalPriceCents = totalPriceCents; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public Long getRelatedSkuId() { return relatedSkuId; }
    public void setRelatedSkuId(Long relatedSkuId) { this.relatedSkuId = relatedSkuId; }
    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public String getServiceAttributesJson() { return serviceAttributesJson; }
    public void setServiceAttributesJson(String serviceAttributesJson) { this.serviceAttributesJson = serviceAttributesJson; }
    public String getDiscountsJson() { return discountsJson; }
    public void setDiscountsJson(String discountsJson) { this.discountsJson = discountsJson; }
}
