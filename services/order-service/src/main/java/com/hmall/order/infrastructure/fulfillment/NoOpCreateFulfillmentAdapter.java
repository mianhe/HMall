package com.hmall.order.infrastructure.fulfillment;

import com.hmall.order.application.port.CreateFulfillmentPort;

/** 占位实现，下游 Fulfillment BC 未接入时使用。由 PortStubConfig 提供 bean。 */
public class NoOpCreateFulfillmentAdapter implements CreateFulfillmentPort {

    @Override
    public void createFulfillment(Long orderId) {
        // 占位：暂不调用 Fulfillment BC
    }
}
