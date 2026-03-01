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
    private String paymentCompleted = "payment.completed";
    private String paymentFailed = "payment.failed";
    private String paymentExpired = "payment.expired";
    private String fulfillmentOrderCreated = "fulfillment.order.created";
    private String fulfillmentOrderAllocated = "fulfillment.order.allocated";
    private String fulfillmentShipped = "fulfillment.shipped";
    private String fulfillmentDelivered = "fulfillment.delivered";
    private String serviceActivated = "fulfillment.service.activated";

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

    public String getPaymentCompleted() {
        return paymentCompleted;
    }

    public void setPaymentCompleted(String paymentCompleted) {
        this.paymentCompleted = paymentCompleted;
    }

    public String getPaymentFailed() {
        return paymentFailed;
    }

    public void setPaymentFailed(String paymentFailed) {
        this.paymentFailed = paymentFailed;
    }

    public String getPaymentExpired() {
        return paymentExpired;
    }

    public void setPaymentExpired(String paymentExpired) {
        this.paymentExpired = paymentExpired;
    }

    public String getFulfillmentOrderCreated() {
        return fulfillmentOrderCreated;
    }

    public void setFulfillmentOrderCreated(String fulfillmentOrderCreated) {
        this.fulfillmentOrderCreated = fulfillmentOrderCreated;
    }

    public String getFulfillmentOrderAllocated() {
        return fulfillmentOrderAllocated;
    }

    public void setFulfillmentOrderAllocated(String fulfillmentOrderAllocated) {
        this.fulfillmentOrderAllocated = fulfillmentOrderAllocated;
    }

    public String getFulfillmentShipped() {
        return fulfillmentShipped;
    }

    public void setFulfillmentShipped(String fulfillmentShipped) {
        this.fulfillmentShipped = fulfillmentShipped;
    }

    public String getFulfillmentDelivered() {
        return fulfillmentDelivered;
    }

    public void setFulfillmentDelivered(String fulfillmentDelivered) {
        this.fulfillmentDelivered = fulfillmentDelivered;
    }

    public String getServiceActivated() {
        return serviceActivated;
    }

    public void setServiceActivated(String serviceActivated) {
        this.serviceActivated = serviceActivated;
    }
}
