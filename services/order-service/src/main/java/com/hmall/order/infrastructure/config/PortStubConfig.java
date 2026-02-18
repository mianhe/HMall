package com.hmall.order.infrastructure.config;

import com.hmall.order.application.port.*;
import com.hmall.order.infrastructure.fulfillment.NoOpCreateFulfillmentAdapter;
import com.hmall.order.infrastructure.inventory.NoOpOccupyInventoryAdapter;
import com.hmall.order.infrastructure.inventory.NoOpReleaseInventoryAdapter;
import com.hmall.order.infrastructure.kafka.NoOpOrderOutboundEventPublisher;
import com.hmall.order.infrastructure.payment.NoOpCreatePaymentAdapter;
import com.hmall.order.infrastructure.payment.NoOpRefundPaymentAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 下游端口的 NoOp 桩实现。
 * 有真实适配器（如 RestOccupyInventoryAdapter、KafkaOrderOutboundEventPublisher）时，由其 @Primary 覆盖。
 */
@Configuration
public class PortStubConfig {

    @Bean
    @ConditionalOnMissingBean(OccupyInventoryPort.class)
    public OccupyInventoryPort occupyInventoryPort() {
        return new NoOpOccupyInventoryAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(ReleaseInventoryPort.class)
    public ReleaseInventoryPort releaseInventoryPort() {
        return new NoOpReleaseInventoryAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(CreatePaymentPort.class)
    public CreatePaymentPort createPaymentPort() {
        return new NoOpCreatePaymentAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(RefundPaymentPort.class)
    public RefundPaymentPort refundPaymentPort() {
        return new NoOpRefundPaymentAdapter();
    }

    @Bean
    public CreateFulfillmentPort createFulfillmentPort() {
        return new NoOpCreateFulfillmentAdapter();
    }

    @Bean(name = "noOpOrderOutboundEventPublisher")
    public OrderOutboundEventPublisher noOpOrderOutboundEventPublisher() {
        return new NoOpOrderOutboundEventPublisher();
    }
}
