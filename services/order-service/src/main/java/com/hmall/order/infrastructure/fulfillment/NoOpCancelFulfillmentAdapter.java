package com.hmall.order.infrastructure.fulfillment;

import com.hmall.order.application.port.CancelFulfillmentPort;

/** 占位实现，下游 Fulfillment BC 未接入时使用。由 PortStubConfig 提供 bean。 */
public class NoOpCancelFulfillmentAdapter implements CancelFulfillmentPort {

    @Override
    public void cancelFulfillment(Long orderId) {
    }
}
