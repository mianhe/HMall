package com.hmall.fulfillment.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fulfillment.kafka.topic")
public class FulfillmentKafkaProperties {

    private String fulfillmentOrderCreated = "fulfillment.order.created";
    private String fulfillmentOrderAllocated = "fulfillment.order.allocated";
    private String fulfillmentShipped = "fulfillment.shipped";
    private String fulfillmentDelivered = "fulfillment.delivered";

    public String getFulfillmentOrderCreated() { return fulfillmentOrderCreated; }
    public void setFulfillmentOrderCreated(String fulfillmentOrderCreated) { this.fulfillmentOrderCreated = fulfillmentOrderCreated; }
    public String getFulfillmentOrderAllocated() { return fulfillmentOrderAllocated; }
    public void setFulfillmentOrderAllocated(String fulfillmentOrderAllocated) { this.fulfillmentOrderAllocated = fulfillmentOrderAllocated; }
    public String getFulfillmentShipped() { return fulfillmentShipped; }
    public void setFulfillmentShipped(String fulfillmentShipped) { this.fulfillmentShipped = fulfillmentShipped; }
    public String getFulfillmentDelivered() { return fulfillmentDelivered; }
    public void setFulfillmentDelivered(String fulfillmentDelivered) { this.fulfillmentDelivered = fulfillmentDelivered; }
}
