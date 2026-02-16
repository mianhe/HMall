package com.hmall.order.acceptance.config;

/**
 * 测试间共享：最近创建的订单 ID，供取消等步骤使用。
 */
public class LastOrderContext {

    private Long lastOrderId;

    public Long getLastOrderId() {
        return lastOrderId;
    }

    public void setLastOrderId(Long lastOrderId) {
        this.lastOrderId = lastOrderId;
    }
}
