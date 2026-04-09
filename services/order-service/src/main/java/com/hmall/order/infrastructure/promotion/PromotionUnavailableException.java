package com.hmall.order.infrastructure.promotion;

public class PromotionUnavailableException extends RuntimeException {

    public PromotionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
