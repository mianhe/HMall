package com.hmall.cart.acceptance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 验收测试共享上下文：跨 Step Definition 类共享用户状态、最后响应状态码、购物车项 ID 列表等。
 */
public class CartTestContext {

    private int lastStatusCode;
    private Long currentUserId;
    private Long lastCartItemId;
    private final List<Long> cartItemIds = new ArrayList<>();
    private Map<String, Object> lastCartItemResponse;

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public Long getLastCartItemId() {
        return lastCartItemId;
    }

    public void setLastCartItemId(Long lastCartItemId) {
        this.lastCartItemId = lastCartItemId;
    }

    public List<Long> getCartItemIds() {
        return cartItemIds;
    }

    public void addCartItemId(Long cartItemId) {
        this.cartItemIds.add(cartItemId);
    }

    public void clearCartItemIds() {
        this.cartItemIds.clear();
    }

    public Map<String, Object> getLastCartItemResponse() {
        return lastCartItemResponse;
    }

    public void setLastCartItemResponse(Map<String, Object> lastCartItemResponse) {
        this.lastCartItemResponse = lastCartItemResponse;
    }
}
