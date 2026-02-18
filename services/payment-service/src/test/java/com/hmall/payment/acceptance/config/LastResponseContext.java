package com.hmall.payment.acceptance.config;

public class LastResponseContext {

    private int lastStatusCode;
    private String lastResponseBody;

    public int getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(int lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

    public String getLastResponseBody() {
        return lastResponseBody;
    }

    public void setLastResponseBody(String lastResponseBody) {
        this.lastResponseBody = lastResponseBody;
    }
}
