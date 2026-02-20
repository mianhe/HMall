package com.hmall.cart.application;

public class SkuNotFoundException extends RuntimeException {

    public SkuNotFoundException(Long skuId) {
        super("SKU 不存在: " + skuId);
    }
}
