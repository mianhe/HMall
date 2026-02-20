package com.hmall.cart.application;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(Long userId) {
        super("购物车不存在: userId=" + userId);
    }
}
