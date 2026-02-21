package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.infrastructure.persistence.FulfillmentOrderJpaRepository;
import io.cucumber.java.Before;

public class DatabaseResetHook {

    private final FulfillmentOrderJpaRepository fulfillmentOrderJpaRepository;
    private final EventCapture eventCapture;
    private final FulfillmentTestContext context;

    public DatabaseResetHook(FulfillmentOrderJpaRepository fulfillmentOrderJpaRepository,
                             EventCapture eventCapture,
                             FulfillmentTestContext context) {
        this.fulfillmentOrderJpaRepository = fulfillmentOrderJpaRepository;
        this.eventCapture = eventCapture;
        this.context = context;
    }

    @Before
    public void reset() {
        fulfillmentOrderJpaRepository.deleteAll();
        eventCapture.clear();
        context.setLastResponseBody(null);
        context.setLastFulfillmentOrderId(null);
        context.setFirstFulfillmentOrderIds(null);
    }
}
