package com.hmall.cart.api;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("用户未登录");
    }
}
