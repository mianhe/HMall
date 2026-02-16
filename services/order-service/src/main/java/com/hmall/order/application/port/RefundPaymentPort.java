package com.hmall.order.application.port;

/** 同步退款。Order 取消已支付订单时调用。 */
public interface RefundPaymentPort {

    void refund(Long orderId);
}
