package com.hmall.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sku_stock")
public class SkuStockEntity {

    @Id
    @Column(name = "sku_id")
    private Long skuId;

    @Column(nullable = false)
    private int available;

    @Column(nullable = false)
    private int reserved;

    public SkuStockEntity() {}

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }
}
