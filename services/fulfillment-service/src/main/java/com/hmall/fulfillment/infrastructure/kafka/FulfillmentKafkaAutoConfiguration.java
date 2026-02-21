package com.hmall.fulfillment.infrastructure.kafka;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 在 KafkaAutoConfiguration 之后注册 Fulfillment 的 Kafka 生产者 Bean。
 * 当 KafkaTemplate 不存在（测试中排除 KafkaAutoConfiguration）时整体跳过。
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnBean(KafkaTemplate.class)
public class FulfillmentKafkaAutoConfiguration {

    @Bean
    @Primary
    public DomainEventPublisher kafkaDomainEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            FulfillmentKafkaProperties kafkaProperties) {
        return new KafkaDomainEventPublisher(kafkaTemplate, kafkaProperties);
    }
}
