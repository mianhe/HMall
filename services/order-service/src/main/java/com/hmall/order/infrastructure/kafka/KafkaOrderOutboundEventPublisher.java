package com.hmall.order.infrastructure.kafka;

import com.hmall.order.application.event.OrderCancelledEvent;
import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.event.OrderCreatedEvent;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/** 发布 Order 出站事件到 Kafka。由 OrderKafkaAutoConfiguration 注册并标记为 @Primary。 */
public class KafkaOrderOutboundEventPublisher implements OrderOutboundEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderOutboundEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderKafkaProperties kafkaProperties;

    public KafkaOrderOutboundEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                            OrderKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send(kafkaProperties.getOrderCreated(), String.valueOf(event.orderId()),
                OrderCreatedMessage.from(event.orderId(), event.items()));
    }

    @Override
    public void publish(OrderCancelledEvent event) {
        kafkaTemplate.send(kafkaProperties.getOrderCancelled(), String.valueOf(event.orderId()),
                OrderCancelledMessage.from(event.orderId()));
    }

    @Override
    public void publish(OrderCompletedEvent event) {
        kafkaTemplate.send(kafkaProperties.getOrderCompleted(), String.valueOf(event.orderId()),
                OrderCompletedMessage.from(event.orderId()));
    }
}
