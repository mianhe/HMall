package com.hmall.order.infrastructure.inventory;

import com.hmall.order.application.port.OccupyInventoryPort;

import java.util.List;

/** 占位实现，下游 Inventory BC 未接入时使用。 */
public class NoOpOccupyInventoryAdapter implements OccupyInventoryPort {

    @Override
    public void occupy(Long orderId, List<ItemQuantity> items) {
        // 占位：始终成功
    }
}
