package com.hmall.order.acceptance.config;

import com.hmall.order.application.port.OccupyInventoryPort;

import java.util.List;

/** 验收测试用 Inventory 占用桩，可配置为模拟库存不足。 */
public class OccupyInventoryStub implements OccupyInventoryPort {

    private volatile boolean shouldFail;

    public void setShouldFail(boolean fail) {
        this.shouldFail = fail;
    }

    public void reset() {
        this.shouldFail = false;
    }

    @Override
    public void occupy(Long orderId, List<OccupyInventoryPort.ItemQuantity> items) {
        if (shouldFail) {
            throw new RuntimeException("库存不足");
        }
    }
}
