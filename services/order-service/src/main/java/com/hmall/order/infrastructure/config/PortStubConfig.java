package com.hmall.order.infrastructure.config;

import com.hmall.order.application.port.*;
import com.hmall.order.infrastructure.fulfillment.NoOpCreateFulfillmentAdapter;
import com.hmall.order.infrastructure.inventory.NoOpOccupyInventoryAdapter;
import com.hmall.order.infrastructure.inventory.NoOpReleaseInventoryAdapter;
import com.hmall.order.infrastructure.payment.NoOpCreatePaymentAdapter;
import com.hmall.order.infrastructure.payment.NoOpRefundPaymentAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 下游 Inventory、Payment、Fulfillment 未接入时的桩实现。 */
@Configuration
public class PortStubConfig {

    @Bean
    public OccupyInventoryPort occupyInventoryPort() {
        return new NoOpOccupyInventoryAdapter();
    }

    @Bean
    public ReleaseInventoryPort releaseInventoryPort() {
        return new NoOpReleaseInventoryAdapter();
    }

    @Bean
    public CreatePaymentPort createPaymentPort() {
        return new NoOpCreatePaymentAdapter();
    }

    @Bean
    public RefundPaymentPort refundPaymentPort() {
        return new NoOpRefundPaymentAdapter();
    }

    @Bean
    public CreateFulfillmentPort createFulfillmentPort() {
        return new NoOpCreateFulfillmentAdapter();
    }
}
