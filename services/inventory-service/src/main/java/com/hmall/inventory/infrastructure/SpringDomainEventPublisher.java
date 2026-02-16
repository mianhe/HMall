package com.hmall.inventory.infrastructure;

import com.hmall.inventory.domain.DomainEventPublisher;
import com.hmall.inventory.domain.StockReleased;
import com.hmall.inventory.domain.StockReserved;
import com.hmall.inventory.infrastructure.kafka.InventoryKafkaProperties;
import com.hmall.inventory.infrastructure.kafka.StockReleasedMessage;
import com.hmall.inventory.infrastructure.kafka.StockReservedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 领域事件只发往 Kafka，供进程外/其他应用订阅。测试时由 EventCapture 替身注入，不连 Kafka。
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryKafkaProperties kafkaProperties;

    public SpringDomainEventPublisher(
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate,
            @Autowired(required = false) InventoryKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @Override
    public void publish(StockReserved event) {
        if (kafkaTemplate == null || kafkaProperties == null) {
            return;
        }
        StockReservedMessage message = StockReservedMessage.from(event.orderId(), event.items(), event.occurredAt());
        try {
            kafkaTemplate.send(kafkaProperties.getStockReserved(), String.valueOf(event.orderId()), message);
        } catch (Exception e) {
            log.error("发送 StockReserved 到 Kafka 失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }

    @Override
    public void publish(StockReleased event) {
        if (kafkaTemplate == null || kafkaProperties == null) {
            return;
        }
        StockReleasedMessage message = StockReleasedMessage.from(event.orderId(), event.occurredAt());
        try {
            kafkaTemplate.send(kafkaProperties.getStockReleased(), String.valueOf(event.orderId()), message);
        } catch (Exception e) {
            log.error("发送 StockReleased 到 Kafka 失败: orderId={}", event.orderId(), e);
            throw e;
        }
    }
}
