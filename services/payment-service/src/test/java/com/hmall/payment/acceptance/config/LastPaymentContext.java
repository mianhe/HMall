package com.hmall.payment.acceptance.config;

/** 验收场景中最近一次创建或使用的支付单 ID，供回调等步骤引用。 */
public class LastPaymentContext {

    private Long lastPaymentId;

    public Long getLastPaymentId() {
        return lastPaymentId;
    }

    public void setLastPaymentId(Long lastPaymentId) {
        this.lastPaymentId = lastPaymentId;
    }
}
