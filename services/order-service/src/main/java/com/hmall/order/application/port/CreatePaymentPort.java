package com.hmall.order.application.port;

/** 同步创建支付单、发起支付流程。Order 在 placeOrder 成功后调用。 */
public interface CreatePaymentPort {

    void createPayment(Long orderId, long amountCents);
}
