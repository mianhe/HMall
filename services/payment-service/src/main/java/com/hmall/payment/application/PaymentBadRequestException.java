package com.hmall.payment.application;

public class PaymentBadRequestException extends RuntimeException {

    public PaymentBadRequestException(String message) {
        super(message);
    }
}
