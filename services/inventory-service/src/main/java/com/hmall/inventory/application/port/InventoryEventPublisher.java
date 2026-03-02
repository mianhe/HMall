package com.hmall.inventory.application.port;

import com.hmall.inventory.domain.StockReleased;
import com.hmall.inventory.domain.StockReserved;

/**
 * 领域事件发布端口。实现由基础设施层提供（Kafka）。
 */
public interface InventoryEventPublisher {

    void publish(StockReserved event);

    void publish(StockReleased event);
}
