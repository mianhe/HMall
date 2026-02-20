package com.hmall.cart.application;

public class CartItemNotFoundAppException extends RuntimeException {

    public CartItemNotFoundAppException(Long cartItemId) {
        super("购物车项不存在: " + cartItemId);
    }
}
