package com.hmall.order.infrastructure.kafka;

import com.hmall.order.application.event.OrderCancelledEvent;
import com.hmall.order.application.event.OrderCompletedEvent;
import com.hmall.order.application.event.OrderCreatedEvent;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Order 出站事件只发往 Kafka，供进程外/其他应用订阅。测试排除 Kafka 时由 Recorder 替身注入。
 */
@Component
@ConditionalOnBean(KafkaTemplate.class)
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
        try {
            OrderCreatedMessage message = OrderCreatedMessage.from(event.orderId(), event.items());
            kafkaTemplate.send(kafkaProperties.getOrderCreated(), String.valueOf(event.orderId()), message);
        } catch (Exception e) {
            log.error("发送 OrderCreated 到 Kafka 失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @Override
    public void publish(OrderCancelledEvent event) {
        try {
            OrderCancelledMessage message = OrderCancelledMessage.from(event.orderId());
            kafkaTemplate.send(kafkaProperties.getOrderCancelled(), String.valueOf(event.orderId()), message);
        } catch (Exception e) {
            log.error("发送 OrderCancelled 到 Kafka 失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @Override
    public void publish(OrderCompletedEvent event) {
        try {
            OrderCompletedMessage message = OrderCompletedMessage.from(event.orderId());
            kafkaTemplate.send(kafkaProperties.getOrderCompleted(), String.valueOf(event.orderId()), message);
        } catch (Exception e) {
            log.error("发送 OrderCompleted 到 Kafka 失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }
}
