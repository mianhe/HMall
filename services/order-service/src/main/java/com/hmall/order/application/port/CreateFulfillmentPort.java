package com.hmall.order.application.port;

/** 创建履约单。Order 收到 PaymentCompleted 后调用。 */
public interface CreateFulfillmentPort {

    void createFulfillment(Long orderId);
}
