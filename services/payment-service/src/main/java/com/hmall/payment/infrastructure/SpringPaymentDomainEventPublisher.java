package com.hmall.payment.infrastructure;

import com.hmall.payment.application.event.PaymentCompletedEvent;
import com.hmall.payment.application.event.PaymentExpiredEvent;
import com.hmall.payment.application.event.PaymentFailedEvent;
import com.hmall.payment.application.port.PaymentDomainEventPublisher;
import com.hmall.payment.infrastructure.kafka.PaymentCompletedMessage;
import com.hmall.payment.infrastructure.kafka.PaymentExpiredMessage;
import com.hmall.payment.infrastructure.kafka.PaymentFailedMessage;
import com.hmall.payment.infrastructure.kafka.PaymentKafkaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Payment 领域事件发布：发到 Kafka 供 Order 等外部应用消费。
 * 无 Kafka（测试、本地开发）时静默跳过。
 */
@Component
public class SpringPaymentDomainEventPublisher implements PaymentDomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringPaymentDomainEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentKafkaProperties kafkaProperties;

    public SpringPaymentDomainEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Autowired(required = false) PaymentKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public void publish(PaymentCompletedEvent event) {
        sendToKafka(kafkaProperties != null ? kafkaProperties.getPaymentCompleted() : null,
                String.valueOf(event.orderId()),
                PaymentCompletedMessage.from(event.orderId(), event.paymentId(), event.occurredAt()));
    }

    @Override
    public void publish(PaymentFailedEvent event) {
        sendToKafka(kafkaProperties != null ? kafkaProperties.getPaymentFailed() : null,
                String.valueOf(event.orderId()),
                PaymentFailedMessage.from(event.orderId(), event.occurredAt()));
    }

    @Override
    public void publish(PaymentExpiredEvent event) {
        sendToKafka(kafkaProperties != null ? kafkaProperties.getPaymentExpired() : null,
                String.valueOf(event.orderId()),
                PaymentExpiredMessage.from(event.orderId(), event.occurredAt()));
    }

    private void sendToKafka(String topic, String key, Object message) {
        if (kafkaTemplate == null || topic == null) {
            return;
        }
        try {
            kafkaTemplate.send(topic, key, message);
        } catch (Exception e) {
            log.error("发送 Payment 事件到 Kafka 失败: topic={}, key={}", topic, key, e);
        }
    }
}
