package com.hmall.order.infrastructure.payment;

import com.hmall.order.application.port.RefundPaymentPort;

/** 占位实现，下游 Payment BC 未接入时使用。 */
public class NoOpRefundPaymentAdapter implements RefundPaymentPort {

    @Override
    public void refund(Long orderId) {
        // 占位：空操作
    }
}
