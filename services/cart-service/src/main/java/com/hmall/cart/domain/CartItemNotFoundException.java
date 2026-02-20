package com.hmall.cart.domain;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long cartItemId) {
        super("购物车项不存在: " + cartItemId);
    }
}
