package com.hmall.order.acceptance.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 记录事件驱动场景中的出站调用（如创建履约单），供 Step Definitions 断言。Order 出站事件由 OrderEventCapture 统一记录。 */
public class EventInvocationRecorder {

    private final List<Long> createFulfillmentOrderIds = new CopyOnWriteArrayList<>();

    public void recordCreateFulfillment(Long orderId) {
        createFulfillmentOrderIds.add(orderId);
    }

    public List<Long> getCreateFulfillmentOrderIds() {
        return Collections.unmodifiableList(new ArrayList<>(createFulfillmentOrderIds));
    }

    public void reset() {
        createFulfillmentOrderIds.clear();
    }
}
