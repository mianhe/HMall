package com.hmall.cart.application;

/**
 * 业务校验失败（如数量非法、SKU 不存在），映射为 400。
 */
public class CartBadRequestException extends RuntimeException {

    public CartBadRequestException(String message) {
        super(message);
    }
}
