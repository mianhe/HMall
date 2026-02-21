package com.hmall.order.infrastructure.fulfillment;

public class FulfillmentUnavailableException extends RuntimeException {
    public FulfillmentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
