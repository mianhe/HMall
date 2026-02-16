package com.hmall.inventory.domain;

/**
 * 领域事件发布端口。实现由基础设施提供（如 Spring Event 或消息总线）。
 */
public interface DomainEventPublisher {

    void publish(StockReserved event);

    void publish(StockReleased event);
}
