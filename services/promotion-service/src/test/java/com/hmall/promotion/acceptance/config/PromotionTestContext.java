package com.hmall.promotion.acceptance.config;

import java.util.Map;

public class PromotionTestContext {

    private int lastStatusCode;
    private Map lastResponseBody;
    private Long lastTemplateId;

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    @SuppressWarnings("unchecked")
    public Map getLastResponseBody() {
        return lastResponseBody;
    }

    public void setLastResponseBody(Map lastResponseBody) {
        this.lastResponseBody = lastResponseBody;
    }

    public Long getLastTemplateId() {
        return lastTemplateId;
    }

    public void setLastTemplateId(Long lastTemplateId) {
        this.lastTemplateId = lastTemplateId;
    }

    private Long lastCouponId;

    public Long getLastCouponId() {
        return lastCouponId;
    }

    public void setLastCouponId(Long lastCouponId) {
        this.lastCouponId = lastCouponId;
    }
}
