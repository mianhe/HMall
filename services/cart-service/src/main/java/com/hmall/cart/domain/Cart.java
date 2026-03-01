package com.hmall.cart.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 购物车聚合根。每个用户一个 Cart，首次操作自动创建。
 * 不快照 SKU 价格——展示时实时拉取 Catalog。
 */
public class Cart {

    private final Long cartId;
    private final Long userId;
    private Instant updatedAt;
    private final List<CartItem> items;

    /** 新建（尚未持久化） */
    public Cart(Long userId) {
        this.cartId = null;
        this.userId = Objects.requireNonNull(userId, "userId");
        this.updatedAt = Instant.now();
        this.items = new ArrayList<>();
    }

    /** 从持久化还原 */
    public Cart(Long cartId, Long userId, Instant updatedAt, List<CartItem> items) {
        this.cartId = Objects.requireNonNull(cartId, "cartId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.updatedAt = updatedAt;
        this.items = new ArrayList<>(items);
    }

    /**
     * 添加商品。若该 skuId 已在购物车中，累加 quantity；否则新增 CartItem。
     * @return 被添加或累加后的 CartItem
     */
    public CartItem addItem(Long skuId, Long relatedSkuId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须 > 0");
        }
        Optional<CartItem> existing = findItemBySkuIdAndRelatedSkuId(skuId, relatedSkuId);
        if (existing.isPresent()) {
            existing.get().increaseQuantity(quantity);
            this.updatedAt = Instant.now();
            return existing.get();
        }
        CartItem item = new CartItem(skuId, relatedSkuId, quantity);
        items.add(item);
        this.updatedAt = Instant.now();
        return item;
    }

    /**
     * 更新指定购物车项的数量。若 quantity = 0 则删除该项；quantity < 0 抛异常。
     * @return 更新后的 CartItem，若删除则返回 null
     */
    public CartItem updateItemQuantity(Long cartItemId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("数量不能为负数");
        }
        CartItem item = findItemById(cartItemId);
        if (quantity == 0) {
            removeItemAndRelated(item);
            this.updatedAt = Instant.now();
            return null;
        }
        item.setQuantity(quantity);
        syncRelatedServiceQuantitiesIfPrimary(item);
        this.updatedAt = Instant.now();
        return item;
    }

    public void removeItem(Long cartItemId) {
        CartItem item = findItemById(cartItemId);
        removeItemAndRelated(item);
        this.updatedAt = Instant.now();
    }

    public void removeItems(List<Long> cartItemIds) {
        List<CartItem> toRemove = cartItemIds.stream()
            .map(this::findItemById)
            .toList();
        List<Long> removedPrimarySkuIds = toRemove.stream()
            .map(CartItem::getSkuId)
            .toList();
        for (Long id : cartItemIds) {
            CartItem item = findItemById(id);
            items.remove(item);
        }
        items.removeIf(i -> i.getRelatedSkuId() != null && removedPrimarySkuIds.contains(i.getRelatedSkuId()));
        this.updatedAt = Instant.now();
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Long getCartId() {
        return cartId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private CartItem findItemById(Long cartItemId) {
        return items.stream()
            .filter(i -> i.getCartItemId() != null && i.getCartItemId().equals(cartItemId))
            .findFirst()
            .orElseThrow(() -> new CartItemNotFoundException(cartItemId));
    }

    private Optional<CartItem> findItemBySkuIdAndRelatedSkuId(Long skuId, Long relatedSkuId) {
        return items.stream()
            .filter(i -> i.getSkuId().equals(skuId) && Objects.equals(i.getRelatedSkuId(), relatedSkuId))
            .findFirst();
    }

    private void syncRelatedServiceQuantitiesIfPrimary(CartItem item) {
        if (item.getRelatedSkuId() != null) {
            return;
        }
        for (CartItem relatedService : items) {
            if (item.getSkuId().equals(relatedService.getRelatedSkuId())) {
                relatedService.setQuantity(item.getQuantity());
            }
        }
    }

    private void removeItemAndRelated(CartItem item) {
        items.remove(item);
        if (item.getRelatedSkuId() != null) {
            return;
        }
        items.removeIf(i -> item.getSkuId().equals(i.getRelatedSkuId()));
    }
}
