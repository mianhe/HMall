package com.hmall.order.infrastructure.inventory;

import com.hmall.order.application.port.ReleaseInventoryPort;

/** 占位实现，下游 Inventory BC 未接入时使用。 */
public class NoOpReleaseInventoryAdapter implements ReleaseInventoryPort {

    @Override
    public void release(Long orderId) {
        // 占位：空操作
    }
}
