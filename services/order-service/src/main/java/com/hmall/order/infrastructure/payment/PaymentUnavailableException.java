package com.hmall.order.infrastructure.payment;

/**
 * Payment 服务不可达（网络/超时）时抛出。
 */
public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
