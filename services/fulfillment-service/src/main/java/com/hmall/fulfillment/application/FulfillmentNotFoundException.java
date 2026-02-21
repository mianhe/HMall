package com.hmall.fulfillment.application;

public class FulfillmentNotFoundException extends RuntimeException {

    public FulfillmentNotFoundException(String message) {
        super(message);
    }
}
