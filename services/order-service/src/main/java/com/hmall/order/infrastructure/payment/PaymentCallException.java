package com.hmall.order.infrastructure.payment;

/**
 * 调用 Payment 服务返回 4xx 时抛出，由应用层捕获并转为业务异常。
 */
public class PaymentCallException extends RuntimeException {

    public PaymentCallException(String message) {
        super(message);
    }

    public PaymentCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
