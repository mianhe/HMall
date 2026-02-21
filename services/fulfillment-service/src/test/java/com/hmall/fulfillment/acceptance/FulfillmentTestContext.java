package com.hmall.fulfillment.acceptance;

import java.util.List;
import java.util.Map;

public class FulfillmentTestContext {

    private int lastStatusCode;
    private Map<String, Object> lastResponseBody;
    private Long lastFulfillmentOrderId;
    private List<Long> firstFulfillmentOrderIds;

    public int getLastStatusCode() { return lastStatusCode; }
    public void setLastStatusCode(int lastStatusCode) { this.lastStatusCode = lastStatusCode; }

    public Map<String, Object> getLastResponseBody() { return lastResponseBody; }
    public void setLastResponseBody(Map<String, Object> lastResponseBody) { this.lastResponseBody = lastResponseBody; }

    public Long getLastFulfillmentOrderId() { return lastFulfillmentOrderId; }
    public void setLastFulfillmentOrderId(Long lastFulfillmentOrderId) { this.lastFulfillmentOrderId = lastFulfillmentOrderId; }

    public List<Long> getFirstFulfillmentOrderIds() { return firstFulfillmentOrderIds; }
    public void setFirstFulfillmentOrderIds(List<Long> firstFulfillmentOrderIds) { this.firstFulfillmentOrderIds = firstFulfillmentOrderIds; }
}
