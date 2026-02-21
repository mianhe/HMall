package com.hmall.order.application.port;

/** 同步取消履约单。Order 取消订单且状态为 FULFILLING 时调用。 */
public interface CancelFulfillmentPort {

    void cancelFulfillment(Long orderId);
}
