package com.hmall.order.infrastructure.fulfillment;

import com.hmall.order.application.port.CreateFulfillmentPort;

import java.util.List;

/** 占位实现，下游 Fulfillment BC 未接入时使用。由 PortStubConfig 提供 bean。 */
public class NoOpCreateFulfillmentAdapter implements CreateFulfillmentPort {

    @Override
    public List<Long> createFulfillment(Long orderId, List<ItemQuantity> items,
                                         ShippingAddress shippingAddress) {
        return List.of();
    }
}
