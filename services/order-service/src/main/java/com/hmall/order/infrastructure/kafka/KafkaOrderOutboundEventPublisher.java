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
        String topic = kafkaProperties.getOrderCreated();
        log.info("发布 OrderCreated: orderId={}, topic={}", event.orderId(), topic);
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), OrderCreatedMessage.from(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("OrderCreated 发送失败: orderId={}", event.orderId(), ex);
                    }
                });
    }

    @Override
    public void publish(OrderCancelledEvent event) {
        String topic = kafkaProperties.getOrderCancelled();
        log.info("发布 OrderCancelled: orderId={}, topic={}", event.orderId(), topic);
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), OrderCancelledMessage.from(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("OrderCancelled 发送失败: orderId={}", event.orderId(), ex);
                    }
                });
    }

    @Override
    public void publish(OrderCompletedEvent event) {
        String topic = kafkaProperties.getOrderCompleted();
        log.info("发布 OrderCompleted: orderId={}, topic={}", event.orderId(), topic);
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), OrderCompletedMessage.from(event))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("OrderCompleted 发送失败: orderId={}", event.orderId(), ex);
                    }
                });
    }
}
