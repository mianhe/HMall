package com.hmall.order.infrastructure.promotion;

public class PromotionCallException extends RuntimeException {

    public PromotionCallException(String message) {
        super(message);
    }

    public PromotionCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
