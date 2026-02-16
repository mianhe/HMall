package com.hmall.inventory.domain;

/**
 * 聚合根：按 SKU 的库存。不变式 available ≥ 0, reserved ≥ 0。
 */
public class SkuStock {

    private final Long skuId;
    private int available;
    private int reserved;

    /** 新建（尚未持久化） */
    public SkuStock(Long skuId, int available, int reserved) {
        this.skuId = skuId;
        this.available = available;
        this.reserved = reserved;
        assert available >= 0 && reserved >= 0;
    }

    public Long getSkuId() {
        return skuId;
    }

    public int getAvailable() {
        return available;
    }

    public int getReserved() {
        return reserved;
    }

    /**
     * 占用数量。若 available ≥ quantity 则扣减 available、增加 reserved，返回 true；否则返回 false。
     */
    public boolean occupy(int quantity) {
        if (quantity <= 0) {
            return false;
        }
        if (available < quantity) {
            return false;
        }
        available -= quantity;
        reserved += quantity;
        return true;
    }

    /**
     * 释放数量。调用方保证 quantity ≤ reserved。
     */
    public void release(int quantity) {
        if (quantity <= 0) {
            return;
        }
        reserved -= quantity;
        available += quantity;
    }

    /**
     * 管理后台初始化/更新可用数量。仅改 available，reserved 不变；quantity ≥ 0。
     */
    public void setAvailable(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("available must be >= 0");
        }
        this.available = quantity;
    }
}
