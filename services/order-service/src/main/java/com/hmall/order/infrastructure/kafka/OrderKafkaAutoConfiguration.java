package com.hmall.order.infrastructure.kafka;

import com.hmall.order.application.OrderEventService;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 在 KafkaAutoConfiguration 之后注册 Order 的 Kafka 生产者/消费者 Bean。
 * 当 KafkaTemplate 不存在（测试中排除 KafkaAutoConfiguration）时整体跳过。
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnBean(KafkaTemplate.class)
public class OrderKafkaAutoConfiguration {

    @Bean
    @Primary
    public OrderOutboundEventPublisher kafkaOrderOutboundEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            OrderKafkaProperties kafkaProperties) {
        return new KafkaOrderOutboundEventPublisher(kafkaTemplate, kafkaProperties);
    }

    @Bean
    public KafkaPaymentEventConsumer kafkaPaymentEventConsumer(OrderEventService orderEventService) {
        return new KafkaPaymentEventConsumer(orderEventService);
    }

    @Bean
    public KafkaFulfillmentEventConsumer kafkaFulfillmentEventConsumer(OrderEventService orderEventService) {
        return new KafkaFulfillmentEventConsumer(orderEventService);
    }
}
