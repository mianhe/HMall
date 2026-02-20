package com.hmall.order.infrastructure.kafka;

import com.hmall.order.application.OrderEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;
import java.util.Map;

/**
 * 从 Kafka 消费 Fulfillment 领域事件（FulfillmentOrderCreated / Shipped / Delivered）。
 * 由 OrderKafkaAutoConfiguration 条件注册，测试中排除 KafkaAutoConfiguration 时不创建。
 */
public class KafkaFulfillmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaFulfillmentEventConsumer.class);

    private final OrderEventService orderEventService;

    public KafkaFulfillmentEventConsumer(OrderEventService orderEventService) {
        this.orderEventService = orderEventService;
    }

    @KafkaListener(topics = "${order.kafka.topic.fulfillment-order-created:fulfillment.order.created}",
                   groupId = "${spring.kafka.consumer.group-id:order-service}")
    public void onFulfillmentOrderCreated(Map<String, Object> message) {
        Long orderId = toLong(message.get("orderId"));
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) message.get("fulfillmentOrderIds");
        List<Long> fulfillmentOrderIds = rawIds.stream().map(Number::longValue).toList();
        log.info("收到 FulfillmentOrderCreated: orderId={}, fulfillmentOrderIds={}", orderId, fulfillmentOrderIds);
        orderEventService.onFulfillmentOrderCreated(orderId, fulfillmentOrderIds);
    }

    @KafkaListener(topics = "${order.kafka.topic.fulfillment-shipped:fulfillment.shipped}",
                   groupId = "${spring.kafka.consumer.group-id:order-service}")
    public void onFulfillmentShipped(Map<String, Object> message) {
        Long orderId = toLong(message.get("orderId"));
        log.info("收到 FulfillmentShipped: orderId={}", orderId);
        orderEventService.onFulfillmentShipped(orderId);
    }

    @KafkaListener(topics = "${order.kafka.topic.fulfillment-delivered:fulfillment.delivered}",
                   groupId = "${spring.kafka.consumer.group-id:order-service}")
    public void onFulfillmentDelivered(Map<String, Object> message) {
        Long orderId = toLong(message.get("orderId"));
        log.info("收到 FulfillmentDelivered: orderId={}", orderId);
        orderEventService.onFulfillmentDelivered(orderId);
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
