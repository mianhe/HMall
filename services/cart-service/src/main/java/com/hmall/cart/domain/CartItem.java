package com.hmall.cart.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 购物车项实体。同一 Cart 中 skuId 唯一，quantity 必须 > 0。
 */
public class CartItem {

    private final Long cartItemId;
    private final Long skuId;
    private final Long relatedSkuId;
    private int quantity;
    private final Instant addedAt;

    /** 新建（尚未持久化） */
    public CartItem(Long skuId, Long relatedSkuId, int quantity) {
        this.cartItemId = null;
        this.skuId = Objects.requireNonNull(skuId, "skuId");
        this.relatedSkuId = relatedSkuId;
        setQuantity(quantity);
        this.addedAt = Instant.now();
    }

    /** 从持久化还原 */
    public CartItem(Long cartItemId, Long skuId, Long relatedSkuId, int quantity, Instant addedAt) {
        this.cartItemId = Objects.requireNonNull(cartItemId, "cartItemId");
        this.skuId = Objects.requireNonNull(skuId, "skuId");
        this.relatedSkuId = relatedSkuId;
        setQuantity(quantity);
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt");
    }

    public void increaseQuantity(int delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("增加数量必须 > 0");
        }
        this.quantity += delta;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须 > 0");
        }
        this.quantity = quantity;
    }

    public Long getCartItemId() {
        return cartItemId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public Long getRelatedSkuId() {
        return relatedSkuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
