package com.hmall.inventory.acceptance;

import java.util.Map;

/**
 * 最后一次 HTTP 响应状态码及库存接口响应体，供通用断言步骤使用。
 */
public class LastResponseContext {

    private volatile int lastStatusCode = -1;
    private volatile Map<String, Object> lastStockBody;

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    public Map<String, Object> getLastStockBody() {
        return lastStockBody;
    }

    public void setLastStockBody(Map<String, Object> lastStockBody) {
        this.lastStockBody = lastStockBody;
    }
}
