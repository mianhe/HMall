package com.hmall.fulfillment.infrastructure.kafka;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.FulfillmentDelivered;
import com.hmall.fulfillment.domain.FulfillmentOrderAllocated;
import com.hmall.fulfillment.domain.FulfillmentOrderCreated;
import com.hmall.fulfillment.domain.FulfillmentShipped;
import com.hmall.fulfillment.domain.ServiceActivated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 发布 Fulfillment 领域事件到 Kafka。
 * 由 FulfillmentKafkaAutoConfiguration 注册并标记为 @Primary。
 */
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FulfillmentKafkaProperties props;

    public KafkaDomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                     FulfillmentKafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    @Override
    public void publish(FulfillmentOrderCreated event) {
        var msg = FulfillmentOrderCreatedMessage.from(event);
        kafkaTemplate.send(props.getFulfillmentOrderCreated(), String.valueOf(event.orderId()), msg);
        log.info("Kafka 发布 FulfillmentOrderCreated: orderId={}", event.orderId());
    }

    @Override
    public void publish(FulfillmentOrderAllocated event) {
        var msg = FulfillmentOrderAllocatedMessage.from(event);
        kafkaTemplate.send(props.getFulfillmentOrderAllocated(), String.valueOf(event.orderId()), msg);
        log.info("Kafka 发布 FulfillmentOrderAllocated: orderId={}, fulfillmentOrderId={}", event.orderId(), event.fulfillmentOrderId());
    }

    @Override
    public void publish(FulfillmentShipped event) {
        var msg = FulfillmentShippedMessage.from(event);
        kafkaTemplate.send(props.getFulfillmentShipped(), String.valueOf(event.orderId()), msg);
        log.info("Kafka 发布 FulfillmentShipped: orderId={}, fulfillmentOrderId={}", event.orderId(), event.fulfillmentOrderId());
    }

    @Override
    public void publish(FulfillmentDelivered event) {
        var msg = FulfillmentDeliveredMessage.from(event);
        kafkaTemplate.send(props.getFulfillmentDelivered(), String.valueOf(event.orderId()), msg);
        log.info("Kafka 发布 FulfillmentDelivered: orderId={}, fulfillmentOrderId={}", event.orderId(), event.fulfillmentOrderId());
    }

    @Override
    public void publish(ServiceActivated event) {
        var msg = ServiceActivatedMessage.from(event);
        kafkaTemplate.send(props.getServiceActivated(), String.valueOf(event.orderId()), msg);
        log.info("Kafka 发布 ServiceActivated: orderId={}, fulfillmentOrderId={}, serviceSkuId={}",
            event.orderId(), event.fulfillmentOrderId(), event.serviceSkuId());
    }
}
