package com.hmall.order.application;

public class OrderBadRequestException extends RuntimeException {

    public OrderBadRequestException(String message) {
        super(message);
    }
}
