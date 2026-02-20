package com.hmall.activity.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.activity.application.ActivityApplicationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 在 KafkaAutoConfiguration 之后注册 Activity 的 Kafka 消费者 Bean。
 * 当 KafkaTemplate 不存在（测试中排除 KafkaAutoConfiguration）时整体跳过。
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnBean(KafkaTemplate.class)
public class ActivityKafkaAutoConfiguration {

    @Bean
    public ActivityKafkaEventConsumer activityKafkaEventConsumer(
            ActivityApplicationService applicationService, ObjectMapper objectMapper) {
        return new ActivityKafkaEventConsumer(applicationService, objectMapper);
    }
}
