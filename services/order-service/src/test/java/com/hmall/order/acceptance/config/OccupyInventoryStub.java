package com.hmall.order.acceptance.config;

import com.hmall.order.application.port.OccupyInventoryPort;

import java.util.List;

/** 验收测试用 Inventory 占用桩，可配置为模拟库存不足。 */
public class OccupyInventoryStub implements OccupyInventoryPort {

    private volatile boolean shouldFail;
    private volatile int callCount;
    private volatile int lastItemCount;

    public void setShouldFail(boolean fail) {
        this.shouldFail = fail;
    }

    public void reset() {
        this.shouldFail = false;
        this.callCount = 0;
        this.lastItemCount = 0;
    }

    public int getCallCount() {
        return callCount;
    }

    public int getLastItemCount() {
        return lastItemCount;
    }

    @Override
    public void occupy(Long orderId, List<OccupyInventoryPort.ItemQuantity> items) {
        callCount++;
        lastItemCount = items == null ? 0 : items.size();
        if (shouldFail) {
            throw new RuntimeException("库存不足");
        }
    }
}
