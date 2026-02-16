package com.hmall.order.infrastructure.payment;

import com.hmall.order.application.port.CreatePaymentPort;

/** 占位实现，下游 Payment BC 未接入时使用。 */
public class NoOpCreatePaymentAdapter implements CreatePaymentPort {

    @Override
    public void createPayment(Long orderId, long amountCents) {
        // 占位：暂不调用 Payment BC
    }
}
