package com.hmall.order.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Order 领域事件 Kafka topic 配置。
 */
@ConfigurationProperties(prefix = "order.kafka.topic")
public class OrderKafkaProperties {

    private String orderCreated = "order.created";
    private String orderCancelled = "order.cancelled";
    private String orderCompleted = "order.completed";

    public String getOrderCreated() {
        return orderCreated;
    }

    public void setOrderCreated(String orderCreated) {
        this.orderCreated = orderCreated;
    }

    public String getOrderCancelled() {
        return orderCancelled;
    }

    public void setOrderCancelled(String orderCancelled) {
        this.orderCancelled = orderCancelled;
    }

    public String getOrderCompleted() {
        return orderCompleted;
    }

    public void setOrderCompleted(String orderCompleted) {
        this.orderCompleted = orderCompleted;
    }
}
