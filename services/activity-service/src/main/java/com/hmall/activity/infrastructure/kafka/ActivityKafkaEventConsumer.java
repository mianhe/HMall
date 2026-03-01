package com.hmall.activity.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.application.RecordActivityCommand;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 从 Kafka 消费各 BC 领域事件，转为 BusinessActivity 落库。
 * 由 ActivityKafkaAutoConfiguration 条件注册，测试中排除 KafkaAutoConfiguration 时不创建。
 */
public class ActivityKafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActivityKafkaEventConsumer.class);

    private final ActivityApplicationService applicationService;
    private final ObjectMapper objectMapper;

    public ActivityKafkaEventConsumer(ActivityApplicationService applicationService, ObjectMapper objectMapper) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = {
            "order.created", "order.cancelled", "order.completed",
            "payment.completed", "payment.failed", "payment.expired",
            "inventory.stock.reserved", "inventory.stock.released",
            "fulfillment.order.created", "fulfillment.order.allocated",
            "fulfillment.shipped", "fulfillment.delivered",
            "fulfillment.service.activated"
        },
        groupId = "${spring.kafka.consumer.group-id:activity-service}"
    )
    public void onEvent(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> message = record.value();
        String topic = record.topic();
        String eventType = (String) message.get("eventType");
        Long orderId = toLong(message.get("orderId"));
        Instant occurredAt = parseInstant(message.get("occurredAt"));

        String eventId = deterministicEventId(topic, eventType, orderId, occurredAt);
        String payload = toJson(message);

        log.info("收到事件: topic={}, eventType={}, orderId={}, eventId={}", topic, eventType, orderId, eventId);

        applicationService.record(new RecordActivityCommand(
            eventId, eventType, topic, orderId, payload, occurredAt
        ));
    }

    /**
     * 由 topic + eventType + orderId + occurredAt 生成确定性 UUID，
     * 保证同一条消息重复投递时产生相同 eventId，实现幂等。
     */
    private static String deterministicEventId(String topic, String eventType, Long orderId, Instant occurredAt) {
        String seed = topic + "|" + eventType + "|" + orderId + "|" + occurredAt;
        return UUID.nameUUIDFromBytes(seed.getBytes()).toString();
    }

    private String toJson(Map<String, Object> message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.warn("序列化事件 payload 失败", e);
            return "{}";
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Instant parseInstant(Object value) {
        if (value == null) return Instant.now();
        if (value instanceof Instant i) return i;
        try {
            return Instant.parse(String.valueOf(value));
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
