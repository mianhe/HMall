package com.hmall.order.acceptance.config;

import com.hmall.order.application.port.CreateFulfillmentPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 记录事件驱动场景中的出站调用（如创建/取消履约单），供 Step Definitions 断言。 */
public class EventInvocationRecorder {

    private final List<Long> createFulfillmentOrderIds = new CopyOnWriteArrayList<>();
    private final List<Long> cancelFulfillmentOrderIds = new CopyOnWriteArrayList<>();

    public List<Long> recordCreateFulfillment(Long orderId,
                                               List<CreateFulfillmentPort.ItemQuantity> items,
                                               CreateFulfillmentPort.ShippingAddress addr) {
        createFulfillmentOrderIds.add(orderId);
        return List.of(orderId * 100);
    }

    public void recordCancelFulfillment(Long orderId) {
        cancelFulfillmentOrderIds.add(orderId);
    }

    public List<Long> getCreateFulfillmentOrderIds() {
        return Collections.unmodifiableList(new ArrayList<>(createFulfillmentOrderIds));
    }

    public List<Long> getCancelFulfillmentOrderIds() {
        return Collections.unmodifiableList(new ArrayList<>(cancelFulfillmentOrderIds));
    }

    public void reset() {
        createFulfillmentOrderIds.clear();
        cancelFulfillmentOrderIds.clear();
    }
}
