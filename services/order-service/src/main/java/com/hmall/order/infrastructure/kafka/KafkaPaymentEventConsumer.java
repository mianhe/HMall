package com.hmall.order.infrastructure.kafka;

import com.hmall.order.application.OrderEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;

/**
 * 从 Kafka 消费 Payment 领域事件（PaymentCompleted / PaymentFailed / PaymentExpired）。
 * 由 OrderKafkaAutoConfiguration 条件注册，测试中排除 KafkaAutoConfiguration 时不创建。
 */
public class KafkaPaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventConsumer.class);

    private final OrderEventService orderEventService;

    public KafkaPaymentEventConsumer(OrderEventService orderEventService) {
        this.orderEventService = orderEventService;
    }

    @KafkaListener(topics = "${order.kafka.topic.payment-completed:payment.completed}",
                   groupId = "${spring.kafka.consumer.group-id:order-service}")
    public void onPaymentCompleted(Map<String, Object> message) {
        Long orderId = toLong(message.get("orderId"));
        Long paymentId = toLong(message.get("paymentId"));
        log.info("收到 PaymentCompleted: orderId={}, paymentId={}", orderId, paymentId);
        orderEventService.onPaymentCompleted(orderId, paymentId);
    }

    @KafkaListener(topics = "${order.kafka.topic.payment-failed:payment.failed}",
                   groupId = "${spring.kafka.consumer.group-id:order-service}")
    public void onPaymentFailed(Map<String, Object> message) {
        Long orderId = toLong(message.get("orderId"));
        log.info("收到 PaymentFailed: orderId={}", orderId);
        orderEventService.onPaymentFailed(orderId);
    }

    @KafkaListener(topics = "${order.kafka.topic.payment-expired:payment.expired}",
                   groupId = "${spring.kafka.consumer.group-id:order-service}")
    public void onPaymentExpired(Map<String, Object> message) {
        Long orderId = toLong(message.get("orderId"));
        log.info("收到 PaymentExpired: orderId={}", orderId);
        orderEventService.onPaymentExpired(orderId);
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
