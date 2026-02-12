package com.hmall.catalog.acceptance;

/**
 * 共享的验收测试上下文：持有最后一次 HTTP 响应的状态码。
 * Catalog 与 User 的 Step Definitions 均可写入，供 CommonAssertionStepDefinitions 断言。
 */
public class LastResponseContext {

    private volatile int lastStatusCode = -1;

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int statusCode) {
        this.lastStatusCode = statusCode;
    }
}
